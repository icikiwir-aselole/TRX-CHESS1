package com.troxzy.trxchess

import android.app.Application
import com.troxzy.trxchess.di.AppContainer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TrxApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Single source of truth for the adaptive visual policy: the container
        // derives it from settings + device signals and pushes it into the
        // design system for all views.
        container.appScope.launch {
            container.visualPolicy.collectLatest { policy ->
                container.designSystem.visualPolicy = policy
            }
        }
    }

    override fun onTerminate() {
        container.shutdown()
        super.onTerminate()
    }
}