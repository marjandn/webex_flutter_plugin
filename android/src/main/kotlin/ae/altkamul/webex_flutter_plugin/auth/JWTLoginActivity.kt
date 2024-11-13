package ae.altkamul.webex_flutter_plugin.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import ae.altkamul.webex_flutter_plugin.R
import ae.altkamul.webex_flutter_plugin.WebexCallApp
import ae.altkamul.webex_flutter_plugin.calling.CallActivity
import ae.altkamul.webex_flutter_plugin.databinding.ActivityLoginWithTokenBinding
import ae.altkamul.webex_flutter_plugin.utils.showDialogWithMessage
import org.koin.androidx.viewmodel.ext.android.viewModel

class JWTLoginActivity : AppCompatActivity() {

    lateinit var binding: ActivityLoginWithTokenBinding
    private val loginViewModel: LoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityLoginWithTokenBinding>(this, R.layout.activity_login_with_token)
                .also { binding = it }
                .apply {
                    title.text = getString(R.string.login_jwt)
                    progressLayout.visibility = View.VISIBLE
                    loginButton.setOnClickListener {
                        binding.loginFailedTextView.visibility = View.GONE
                        if (tokenText.text.isEmpty()) {
                            showDialogWithMessage(this@JWTLoginActivity, R.string.error_occurred, resources.getString(R.string.login_token_empty_error))
                        }
                        else {
                            binding.loginButton.visibility = View.GONE
                            progressLayout.visibility = View.VISIBLE
                            val token = tokenText.text.toString()
                            loginViewModel.loginWithJWT(token)
                        }
                    }

                    loginViewModel.isAuthorized.observe(this@JWTLoginActivity, Observer { isAuthorized ->
                        progressLayout.visibility = View.GONE
                        isAuthorized?.let {
                            if (it) {
                                onLoggedIn()
                            } else {
                                onLoginFailed()
                            }
                        }
                    })

                    loginViewModel.isAuthorizedCached.observe(this@JWTLoginActivity, Observer { isAuthorizedCached ->
                        progressLayout.visibility = View.GONE
                        isAuthorizedCached?.let {
                            if (it) {
                                onLoggedIn()
                            } else {
                                tokenText.visibility = View.VISIBLE
                                loginButton.visibility = View.VISIBLE
                                loginFailedTextView.visibility = View.GONE
                            }
                        }
                    })

                    loginViewModel.errorData.observe(this@JWTLoginActivity, Observer { errorMessage ->
                        progressLayout.visibility = View.GONE
                        onLoginFailed(errorMessage)
                    })

                    loginViewModel.initialize()
                }
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
                    "23657226651",
                    false,
                    false
                )
            )
        )
//        startActivity(Intent(this, Dial::class.java))
        finish()
    }

    private fun onLoginFailed(failureMessage: String = getString(R.string.jwt_login_failed)) {
        binding.loginButton.visibility = View.VISIBLE
        binding.loginFailedTextView.visibility = View.VISIBLE
        binding.loginFailedTextView.text = failureMessage
    }
}