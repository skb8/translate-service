package com.skb8.translateservice;

import com.skb8.translateservice.ITranslationCallback;

/**
 * Binder interface exposed by the TranslationBinderService. Other apps can bind to
 * this service (action "com.skb8.translateservice.action.TRANSLATE") and call
 * translate() to get on-device ML Kit translations.
 */
interface ITranslationService {

    /**
     * Translates text.
     *
     * @param text the text to translate
     * @param sourceLanguage BCP-47 language code (e.g. "en"), or "" / "auto" to let
     *        ML Kit Language Identification detect the source language
     * @param targetLanguage BCP-47 language code, or "" to use the user's default
     *        target language configured in the app
     * @param callback receives the result or an error
     */
    void translate(String text, String sourceLanguage, String targetLanguage, ITranslationCallback callback);
}
