package ae.altkamul.webex_flutter_plugin.auth

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import ae.altkamul.webex_flutter_plugin.R
import ae.altkamul.webex_flutter_plugin.WebexViewModel
import ae.altkamul.webex_flutter_plugin.databinding.ActivityLoginBinding
import ae.altkamul.webex_flutter_plugin.utils.SharedPrefUtils.clearEmailPref
import ae.altkamul.webex_flutter_plugin.utils.SharedPrefUtils.getLoginTypePref
import ae.altkamul.webex_flutter_plugin.utils.SharedPrefUtils.saveEmailPref
import ae.altkamul.webex_flutter_plugin.utils.showDialogForInputEmail
import com.ciscowebex.androidsdk.utils.AppConfiguration
import com.ciscowebex.androidsdk.utils.SettingsStore
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.context.loadKoinModules

import ae.altkamul.webex_flutter_plugin.auth.LoginActivity
import ae.altkamul.webex_flutter_plugin.auth.loginModule
import ae.altkamul.webex_flutter_plugin.calling.callModule
import ae.altkamul.webex_flutter_plugin.calling.calendarMeeting.calendarMeetingsModule
import ae.altkamul.webex_flutter_plugin.mainAppModule
import ae.altkamul.webex_flutter_plugin.person.personModule
import ae.altkamul.webex_flutter_plugin.utils.SharedPrefUtils
import ae.altkamul.webex_flutter_plugin.webexModule

class LoginActivity : AppCompatActivity() {

    lateinit var binding: ActivityLoginBinding

    private val webexViewModel: WebexViewModel by viewModel()
 
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppConfiguration.setContext(applicationContext)
        DataBindingUtil.setContentView<ActivityLoginBinding>(this, R.layout.activity_login)
                .also { binding = it }
                .apply {

                    val type = getLoginTypePref(this@LoginActivity)
                    loadModules(type)

                    btnJwtLogin.setOnClickListener {
                        startJWTActivity()
                    } 
                }
    }

    private fun loadModules(type: String?) {
        loadKoinModules(
            listOf(
                mainAppModule,
                webexModule,
                loginModule,
                JWTWebexModule,
                callModule,
                personModule,
                calendarMeetingsModule
            )
        )

        startActivity(Intent(this@LoginActivity, JWTLoginActivity::class.java))
        finish()
    }
 

    private fun toggleButtonsVisibility(hide: Boolean) {
        if (hide) {
            binding.loginButtonLayout.visibility = View.GONE
            binding.loginFailedTextView.visibility = View.GONE
            binding.btnJwtLogin.visibility = View.GONE
        } else {
            binding.loginButtonLayout.visibility = View.VISIBLE
            binding.loginFailedTextView.visibility = View.GONE
            binding.btnJwtLogin.visibility = View.VISIBLE
        }
    }

    private fun startJWTActivity() {
        enableBackgroundConnection()
        startActivity(Intent(this@LoginActivity, JWTLoginActivity::class.java))
        finish()
    }
 

    private fun enableBackgroundConnection() {
        webexViewModel.enableBackgroundConnection(webexViewModel.enableBgConnectiontoggle)
    }

 
}