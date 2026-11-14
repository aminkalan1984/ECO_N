package com.example.data

import android.content.Context

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }
    val preferences: EcoPreferences by lazy { EcoPreferences(appContext) }
    val ecoRepository: EcoRepository by lazy { EcoRepository(preferences, database) }
    val aiRepository: AiRepository by lazy { AiRepository() }
}
