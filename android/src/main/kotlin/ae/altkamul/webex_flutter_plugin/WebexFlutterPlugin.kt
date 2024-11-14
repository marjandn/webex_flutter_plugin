package ae.altkamul.webex_flutter_plugin

import ae.altkamul.webex_flutter_plugin.WebexCallApp.Companion.applicationContext
import ae.altkamul.webex_flutter_plugin.auth.JWTLoginActivity
import ae.altkamul.webex_flutter_plugin.auth.LoginActivity
import android.content.Intent
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat.startActivity
import com.ciscowebex.androidsdk.Webex

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result


/** WebexFlutterPlugin */
class WebexFlutterPlugin : FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(
            flutterPluginBinding.binaryMessenger,
            "webex_flutter_plugin"
        )
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        if (call.method == "getPlatformVersion") {
//      result.success("Android ${android.os.Build.VERSION.RELEASE}")
            val intent =
                Intent(applicationContext(), JWTLoginActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            applicationContext().startActivity(intent)
        } else {
            result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
