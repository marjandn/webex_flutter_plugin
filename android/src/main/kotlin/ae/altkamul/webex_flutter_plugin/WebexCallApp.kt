package ae.altkamul.webex_flutter_plugin

import ae.altkamul.webex_flutter_plugin.auth.JWTWebexModule
import android.app.Application
import android.content.Context
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin


class WebexCallApp : Application(), LifecycleObserver {

    companion object {
        lateinit var instance: WebexCallApp
            private set

        fun applicationContext(): Context {
            return instance.applicationContext
        }

        fun get(): WebexCallApp {
            return instance
        }

   /*     var inForeground: Boolean = false


        // App level boolean to keep track of if the CUCM login is of type SSO Login
        var isUCSSOLogin = false

        var isKoinModulesLoaded: Boolean = false*/

    }

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@WebexCallApp)
        }

        ProcessLifecycleOwner.get().lifecycle.addObserver(this);
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        instance = this
    }

/*    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        // app moved to foreground
        inForeground = true
    }*/

/*    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onMoveToBackground() {
        // app moved to background
        inForeground = false
    }*/

    fun closeApplication() {
        android.os.Process.killProcess(android.os.Process.myPid())
    }




 /*   fun loadKoinModules() {
        loadKoinModules(
            listOf(
                mainAppModule,
                webexModule,
                loginModule,
                JWTWebexModule,
                personModule,
            )
        )
        isKoinModulesLoaded = true
    }*/

    /*fun unloadKoinModules() {
        unloadKoinModules(
            listOf(
                mainAppModule,
                webexModule,
                loginModule,
                JWTWebexModule,
                callModule,
                personModule,
            )
        )
        isKoinModulesLoaded = false
    }*/
}