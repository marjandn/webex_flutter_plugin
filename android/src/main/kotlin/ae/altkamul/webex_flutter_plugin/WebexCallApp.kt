package ae.altkamul.webex_flutter_plugin

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


    fun closeApplication() {
        android.os.Process.killProcess(android.os.Process.myPid())
    }

}