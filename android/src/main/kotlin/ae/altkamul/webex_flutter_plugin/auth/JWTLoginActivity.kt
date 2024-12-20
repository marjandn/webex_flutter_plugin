package ae.altkamul.webex_flutter_plugin.auth

import ae.altkamul.webex_flutter_plugin.Constants
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import ae.altkamul.webex_flutter_plugin.R
import ae.altkamul.webex_flutter_plugin.WebexCallApp
import ae.altkamul.webex_flutter_plugin.WebexViewModel
import ae.altkamul.webex_flutter_plugin.calling.CallActivity
import ae.altkamul.webex_flutter_plugin.databinding.ActivityLoginWithTokenBinding

import ae.altkamul.webex_flutter_plugin.utils.showDialogWithMessage
import ae.altkamul.webex_flutter_plugin.webexModule
import com.ciscowebex.androidsdk.utils.AppConfiguration
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.context.loadKoinModules

class JWTLoginActivity : AppCompatActivity() {

    lateinit var binding: ActivityLoginWithTokenBinding

    private val loginViewModel: LoginViewModel by viewModel()
    private val webexViewModel: WebexViewModel by viewModel()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sentToken =  intent.getStringExtra(Constants.Intent.JWTToken) ?: "";

        enableBackgroundConnection()

        DataBindingUtil.setContentView<ActivityLoginWithTokenBinding>(
            this,
            R.layout.activity_login_with_token
        )
            .also { binding = it }
            .apply {
                title.text = getString(R.string.login_jwt)
                progressLayout.visibility = View.VISIBLE
                loginButton.setOnClickListener {
                    binding.loginFailedTextView.visibility = View.GONE
                    if (tokenText.text.isEmpty()) {
                        showDialogWithMessage(
                            this@JWTLoginActivity,
                            R.string.error_occurred,
                            resources.getString(R.string.login_token_empty_error)
                        )
                    } else {
                        binding.loginButton.visibility = View.GONE
                        progressLayout.visibility = View.VISIBLE
                        val token = tokenText.text.toString()
                        loginViewModel.loginWithJWT(token)
                    }
                }

                loginViewModel.isAuthorized.observe(
                    this@JWTLoginActivity,
                    Observer { isAuthorized ->
                        progressLayout.visibility = View.GONE
                        isAuthorized?.let {
                            if (it) {
                                onLoggedIn()
                            } else {
                                onLoginFailed()
                            }
                        }
                    })

                loginViewModel.isAuthorizedCached.observe(
                    this@JWTLoginActivity,
                    Observer { isAuthorizedCached ->
                        progressLayout.visibility = View.GONE
                        isAuthorizedCached?.let {
                            if (it) {
                                onLoggedIn()
                            } else {
                               /* tokenText.visibility = View.VISIBLE
                                loginButton.visibility = View.VISIBLE
                                loginFailedTextView.visibility = View.GONE*/


                                loginViewModel.loginWithJWT(sentToken)
                            }
                        }
                    })

                loginViewModel.errorData.observe(
                    this@JWTLoginActivity,
                    Observer { errorMessage ->
                        progressLayout.visibility = View.GONE
                        onLoginFailed(errorMessage)
                    })

                loginViewModel.initialize()
            }
    }

    private fun enableBackgroundConnection() {
        loadKoinModules(
            listOf(
                webexModule,
                loginModule,
                JWTWebexModule,

                )
        )
        AppConfiguration.setContext(applicationContext)

        webexViewModel.enableBackgroundConnection(webexViewModel.enableBgConnectiontoggle)
    }

    override fun onBackPressed() {
        (application as WebexCallApp).closeApplication()
        super.onBackPressed()
    }

    private fun onLoggedIn() {
        startActivity(
            Intent(
                CallActivity.getOutgoingIntent(
                    this,
                    intent.getStringExtra(Constants.Intent.OUTGOING_CALL_CALLER_ID)
                        ?: "-"
                )
            )
        )

        finish()
    }

    private fun onLoginFailed(failureMessage: String = getString(R.string.jwt_login_failed)) {
        binding.loginButton.visibility = View.VISIBLE
        binding.loginFailedTextView.visibility = View.VISIBLE
        binding.loginFailedTextView.text = failureMessage
    }
}