package com.skb8.translateservice;

/**
 * Callback used by clients of {@link ITranslationService#translate} to receive
 * the translation result asynchronously.
 */
oneway interface ITranslationCallback {

    /** Called when the translation finished successfully. */
    void onResult(String translatedText, String detectedSourceLanguage);

    /**
     * Called when the translation could not be completed.
     *
     * errorCode is one of:
     *  - "MODEL_DOWNLOAD_REQUIRED": the on-device model for the requested language
     *    is missing and the device is not on Wi-Fi, so the user must approve the
     *    download from the host app's notification.
     *  - "LANGUAGE_NOT_SUPPORTED": ML Kit cannot translate the requested language pair.
     *  - "TRANSLATION_FAILED": an unexpected error occurred while translating.
     */
    void onError(String errorCode, String message);
}
