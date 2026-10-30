package com.example.techfeed.feature.translation.di

import com.example.techfeed.feature.translation.data.ArticleTranslator
import com.example.techfeed.feature.translation.domain.TranslationService
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@Module
@InstallIn(ViewModelComponent::class)
object TranslationModule {
    @Provides
    fun provideTranslationService(translator: ArticleTranslator): TranslationService = translator

    @Provides
    @ViewModelScoped
    fun provideTranslator(): Translator =
        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.JAPANESE)
                .build(),
        )
}
