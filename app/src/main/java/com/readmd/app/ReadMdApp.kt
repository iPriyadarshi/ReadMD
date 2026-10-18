package com.readmd.app

import android.app.Application
import com.readmd.app.di.AppContainer

class ReadMdApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
