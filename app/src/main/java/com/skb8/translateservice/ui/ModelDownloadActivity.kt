package com.skb8.translateservice.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.skb8.translateservice.data.SettingsRepository
import kotlinx.coroutines.launch

/**
 * Transparent, no-UI activity launched from the model-download notification's
 * Allow/Deny actions. Persists the user's decision and finishes immediately;
 * the calling app should retry its translate() request afterwards.
 */
class ModelDownloadActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_LANGUAGE = "language"
        private const val EXTRA_APPROVE = "approve"

        fun intent(context: Context, mlKitLanguage: String, approve: Boolean): Intent =
            Intent(context, ModelDownloadActivity::class.java)
                .putExtra(EXTRA_LANGUAGE, mlKitLanguage)
                .putExtra(EXTRA_APPROVE, approve)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val language = intent.getStringExtra(EXTRA_LANGUAGE)
        val approve = intent.getBooleanExtra(EXTRA_APPROVE, false)

        if (language != null && approve) {
            val settings = SettingsRepository(applicationContext)
            lifecycleScope.launch {
                settings.approveMobileDataDownload(language)
                Toast.makeText(
                    applicationContext,
                    "Model \"$language\" will download over mobile data on the next request",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        } else {
            Toast.makeText(applicationContext, "Download declined", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
