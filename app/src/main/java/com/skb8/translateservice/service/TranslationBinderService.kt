package com.skb8.translateservice.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.skb8.translateservice.ITranslationCallback
import com.skb8.translateservice.ITranslationService
import com.skb8.translateservice.R
import com.skb8.translateservice.data.AppDatabase
import com.skb8.translateservice.data.SettingsRepository
import com.skb8.translateservice.data.TranslationLog
import com.skb8.translateservice.data.TranslationStatus
import com.skb8.translateservice.ui.ModelDownloadActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Background service exposing [ITranslationService] over Binder so other apps can request
 * on-device ML Kit translations. Runs as a foreground service so it keeps working while
 * the host app is not in the foreground.
 */
class TranslationBinderService : Service() {

    companion object {
        const val SERVICE_CHANNEL_ID = "translation_service"
        const val DOWNLOAD_CHANNEL_ID = "model_downloads"
        const val SERVICE_NOTIFICATION_ID = 1
        private var nextDownloadNotificationId = 100
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val engine = TranslationEngine()
    private lateinit var settings: SettingsRepository
    private lateinit var logDao: com.skb8.translateservice.data.LogDao

    override fun onCreate() {
        super.onCreate()
        settings = SettingsRepository(applicationContext)
        logDao = AppDatabase.get(applicationContext).logDao()
        createNotificationChannels()
        startForeground(SERVICE_NOTIFICATION_ID, buildServiceNotification())
        logStartup()
    }

    private fun logStartup() {
        scope.launch {
            logAndNotify(
                callerPackage = packageName, callerLabel = "System",
                sourceLanguage = "", targetLanguage = "",
                sourceText = "Translation service started", translatedText = "",
                status = TranslationStatus.INFO, errorMessage = null
            )

            val downloaded = engine.getDownloadedLanguages()
            val modelsText = if (downloaded.isEmpty()) {
                "No ML Kit translation models installed yet"
            } else {
                "Installed ML Kit models: ${downloaded.joinToString(", ")}"
            }
            logAndNotify(
                callerPackage = packageName, callerLabel = "System",
                sourceLanguage = "", targetLanguage = "",
                sourceText = modelsText, translatedText = "",
                status = TranslationStatus.INFO, errorMessage = null
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        engine.close()
        super.onDestroy()
    }

    private val binder = object : ITranslationService.Stub() {
        override fun translate(
            text: String,
            sourceLanguage: String?,
            targetLanguage: String?,
            callback: ITranslationCallback
        ) {
            val callingUid = Binder.getCallingUid()
            val (callerPackage, callerLabel) = resolveCaller(callingUid)

            scope.launch {
                handleTranslate(text, sourceLanguage.orEmpty(), targetLanguage.orEmpty(), callerPackage, callerLabel, callback)
            }
        }
    }

    private suspend fun handleTranslate(
        text: String,
        requestedSource: String,
        requestedTarget: String,
        callerPackage: String,
        callerLabel: String,
        callback: ITranslationCallback
    ) {
        try {
            // 1. Resolve source language.
            val sourceTag = if (requestedSource.isBlank() || requestedSource == "auto") {
                engine.identifyLanguage(text)
            } else {
                requestedSource
            }
            if (sourceTag == null) {
                logAndNotify(
                    callerPackage, callerLabel, requestedSource, requestedTarget, text, "",
                    TranslationStatus.ERROR, "LANGUAGE_NOT_SUPPORTED"
                )
                callback.onError("LANGUAGE_NOT_SUPPORTED", "Could not identify source language")
                return
            }

            // 2. Resolve target language.
            val targetTag = requestedTarget.ifBlank { settings.defaultTargetLanguage.first() }

            val sourceMl = engine.toMlKitLanguage(sourceTag)
            val targetMl = engine.toMlKitLanguage(targetTag)
            if (sourceMl == null || targetMl == null) {
                logAndNotify(
                    callerPackage, callerLabel, sourceTag, targetTag, text, "",
                    TranslationStatus.ERROR, "LANGUAGE_NOT_SUPPORTED"
                )
                callback.onError("LANGUAGE_NOT_SUPPORTED", "Unsupported language pair: $sourceTag -> $targetTag")
                return
            }

            // 3. Ensure models are available, prompting for mobile data if needed.
            val missing = listOf(sourceMl, targetMl).filterNot { engine.isModelDownloaded(it) }
            if (missing.isNotEmpty()) {
                val wifiAvailable = isOnWifi()
                val needsApproval = missing.filterNot { wifiAvailable || settings.isMobileDataApproved(it) }
                if (needsApproval.isNotEmpty()) {
                    val lang = needsApproval.first()
                    logAndNotify(
                        callerPackage, callerLabel, sourceTag, targetTag, text, "",
                        TranslationStatus.PENDING_MODEL, "MODEL_DOWNLOAD_REQUIRED:$lang"
                    )
                    showModelDownloadNotification(callerLabel, lang)
                    callback.onError("MODEL_DOWNLOAD_REQUIRED", lang)
                    return
                }
                missing.forEach { lang ->
                    engine.downloadModel(lang, allowMobileData = !wifiAvailable)
                }
            }

            // 4. Translate.
            val translated = engine.translate(text, sourceMl, targetMl)
            logAndNotify(
                callerPackage, callerLabel, sourceTag, targetTag, text, translated,
                TranslationStatus.OK, null
            )
            callback.onResult(translated, sourceTag)
        } catch (e: Exception) {
            logAndNotify(
                callerPackage, callerLabel, requestedSource, requestedTarget, text, "",
                TranslationStatus.ERROR, e.message
            )
            callback.onError("TRANSLATION_FAILED", e.message ?: "Unknown error")
        }
    }

    private suspend fun logAndNotify(
        callerPackage: String,
        callerLabel: String,
        sourceLanguage: String,
        targetLanguage: String,
        sourceText: String,
        translatedText: String,
        status: TranslationStatus,
        errorMessage: String?
    ) {
        logDao.insert(
            TranslationLog(
                timestamp = System.currentTimeMillis(),
                callerPackage = callerPackage,
                callerLabel = callerLabel,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                sourceText = sourceText,
                translatedText = translatedText,
                status = status,
                errorMessage = errorMessage
            )
        )
    }

    private fun resolveCaller(callingUid: Int): Pair<String, String> {
        val pm = packageManager
        val packageName = pm.getPackagesForUid(callingUid)?.firstOrNull() ?: "uid:$callingUid"
        val label = try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
        return packageName to label
    }

    private fun isOnWifi(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                SERVICE_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_MIN
            ).apply { description = getString(R.string.notification_channel_desc) }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                DOWNLOAD_CHANNEL_ID,
                getString(R.string.notification_download_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    private fun buildServiceNotification(): Notification =
        NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

    private fun showModelDownloadNotification(callerLabel: String, mlKitLanguage: String) {
        val allowIntent = ModelDownloadActivity.intent(this, mlKitLanguage, approve = true)
        val denyIntent = ModelDownloadActivity.intent(this, mlKitLanguage, approve = false)

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val notificationId = nextDownloadNotificationId++

        val allowPending = PendingIntent.getActivity(this, notificationId * 2, allowIntent, flags)
        val denyPending = PendingIntent.getActivity(this, notificationId * 2 + 1, denyIntent, flags)

        val notification = NotificationCompat.Builder(this, DOWNLOAD_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_download_title))
            .setContentText(getString(R.string.notification_download_text, callerLabel, mlKitLanguage))
            .setStyle(NotificationCompat.BigTextStyle().bigText(getString(R.string.notification_download_text, callerLabel, mlKitLanguage)))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .addAction(0, getString(R.string.action_allow), allowPending)
            .addAction(0, getString(R.string.action_deny), denyPending)
            .build()

        getSystemService(NotificationManager::class.java).notify(notificationId, notification)
    }
}
