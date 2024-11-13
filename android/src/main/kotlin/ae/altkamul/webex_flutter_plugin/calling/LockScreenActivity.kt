package ae.altkamul.webex_flutter_plugin.calling

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import ae.altkamul.webex_flutter_plugin.BaseActivity
import ae.altkamul.webex_flutter_plugin.CallRejectService
import ae.altkamul.webex_flutter_plugin.R
import ae.altkamul.webex_flutter_plugin.WebexRepository
import ae.altkamul.webex_flutter_plugin.databinding.ActivityLockScreenBinding
import ae.altkamul.webex_flutter_plugin.utils.Constants
import com.ciscowebex.androidsdk.phone.CallObserver
import org.koin.android.ext.android.bind
import org.koin.android.ext.android.inject

class LockScreenActivity : BaseActivity() {

    lateinit var binding : ActivityLockScreenBinding
    private val repository: WebexRepository by inject()

    companion object {
        fun getLockScreenIntent(context: Context, callId: String? = null): Intent {
            val intent = Intent(context, LockScreenActivity::class.java)
            intent.putExtra(Constants.Intent.CALLING_ACTIVITY_ID, 1)
            intent.putExtra(Constants.Intent.CALL_ID, callId)
            intent.action = Constants.Action.WEBEX_CALL_ACTION
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return intent
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<ActivityLockScreenBinding>(this, R.layout.activity_lock_screen)
                .also { binding = it }
                .apply {
                    val callId = intent.getStringExtra(Constants.Intent.CALL_ID)

                    if(callId != null) {
                        val call = repository.getCall(callId)
                        binding.callerinfo.text = call?.getTitle()

                        call?.let{
                            repository.setCallObserver(it, object : CallObserver {
                                override fun onDisconnected(event: CallObserver.CallDisconnectedEvent?) {
                                    super.onDisconnected(event)
                                    finish()
                                }
                            })
                        }

                        binding.accept.setOnClickListener {
                            val intent = CallActivity.getCallAcceptIntent(this@LockScreenActivity, callId)
                            startActivity(intent)
                            finishAfterTransition()
                        }

                        binding.decline.setOnClickListener {
                            startService(CallRejectService.getCallRejectIntent(this@LockScreenActivity, callId))
                            finish()
                        }
                    }

                }

    }
}