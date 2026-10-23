package com.codecrafters.kehdona

import android.app.Application
import com.codecrafters.kehdona.di.AppContainer
import com.codecrafters.kehdona.di.DefaultAppContainer

class KehDoNaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}
