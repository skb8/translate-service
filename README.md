# Translate Service

Android-приложение с фоновым сервисом, который принимает запросы на перевод от
других приложений через Binder/AIDL и выполняет перевод на устройстве с помощью
**Google ML Kit** (Translation + Language Identification, без интернета после
скачивания языковых моделей).

## Возможности

- Фоновый `Service` (`TranslationBinderService`), запускается как foreground
  service и работает, даже когда основная Activity не открыта.
- Любое приложение может забиндиться к сервису и вызвать `translate(...)`.
- Автоопределение исходного языка через ML Kit Language ID, если он не указан.
- Целевой язык по умолчанию задаётся в UI приложения и сохраняется (DataStore).
- Если нужная языковая модель (~30 МБ) не скачана и устройство не на Wi-Fi,
  сервис покажет уведомление с кнопками "Разрешить"/"Отклонить" для скачивания
  по мобильной сети.
- UI на Material 3 (Jetpack Compose) со списком всех запросов: исходный текст,
  приложение-отправитель, перевод, языки, статус.

## AIDL-интерфейс

```
package com.skb8.translateservice;

interface ITranslationService {
    void translate(String text, String sourceLanguage, String targetLanguage, ITranslationCallback callback);
}

oneway interface ITranslationCallback {
    void onResult(String translatedText, String detectedSourceLanguage);
    void onError(String errorCode, String message);
}
```

- `sourceLanguage`: код языка BCP-47 (например `"en"`), либо `""`/`"auto"` —
  тогда язык определяется автоматически.
- `targetLanguage`: код языка BCP-47, либо `""` — используется язык по
  умолчанию, выбранный пользователем в приложении.
- `errorCode` может быть:
  - `MODEL_DOWNLOAD_REQUIRED` — нужной языковой модели нет, а устройство не на
    Wi-Fi; пользователю показано уведомление для подтверждения скачивания.
    Повторите запрос после подтверждения.
  - `LANGUAGE_NOT_SUPPORTED` — язык не поддерживается ML Kit.
  - `TRANSLATION_FAILED` — иная ошибка перевода.

## Пример клиента (Kotlin)

Скопируйте `.aidl`-файлы из `app/src/main/aidl/com/skb8/translateservice/` в
своё приложение (тот же пакет `com.skb8.translateservice`), затем:

```kotlin
val connection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        val service = ITranslationService.Stub.asInterface(binder)
        service.translate("Hello world", "en", "ru", object : ITranslationCallback.Stub() {
            override fun onResult(translatedText: String, detectedSourceLanguage: String) {
                Log.d("Translate", "Result: $translatedText (from $detectedSourceLanguage)")
            }
            override fun onError(errorCode: String, message: String) {
                Log.e("Translate", "Error $errorCode: $message")
            }
        })
    }

    override fun onServiceDisconnected(name: ComponentName) {}
}

val intent = Intent("com.skb8.translateservice.action.TRANSLATE").apply {
    setPackage("com.skb8.translateservice")
}
context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
```

## Сборка

```bash
./gradlew assembleDebug
```

APK появится в `app/build/outputs/apk/debug/`. CI собирает debug-APK на каждый
push в `main` (см. `.github/workflows/android-build.yml`) и публикует его как
артефакт сборки.

## Требования

- Android 8.0 (API 26) и выше.
- Разрешения: `INTERNET`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`,
  `FOREGROUND_SERVICE_DATA_SYNC`, `POST_NOTIFICATIONS`.
