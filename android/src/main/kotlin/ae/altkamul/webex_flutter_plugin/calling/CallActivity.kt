package ae.altkamul.webex_flutter_plugin.calling

import android.app.AlertDialog
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import ae.altkamul.webex_flutter_plugin.CallControlsFragment
import ae.altkamul.webex_flutter_plugin.Constants
import ae.altkamul.webex_flutter_plugin.R
import ae.altkamul.webex_flutter_plugin.WebexViewModel
import ae.altkamul.webex_flutter_plugin.databinding.ActivityCallBinding
import androidx.appcompat.app.AppCompatActivity
import com.ciscowebex.androidsdk.phone.*
import com.ciscowebex.androidsdk.phone.closedCaptions.CaptionItem
import com.ciscowebex.androidsdk.phone.closedCaptions.ClosedCaptionsInfo
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.getValue


class CallActivity : AppCompatActivity()  {
    val webexViewModel: WebexViewModel by viewModel()

    lateinit var binding: ActivityCallBinding
    private var pictureInPictureParamsBuilder: PictureInPictureParams.Builder? =
        null
    var calls: ArrayList<Call> = ArrayList()

    var argumentList: HashMap<String, Bundle> = HashMap()



    companion object {
        fun getOutgoingIntent(
            context: Context,
            callerName: String
        ): Intent {
            val intent = Intent(context, CallActivity::class.java)
            intent.putExtra(
                Constants.Intent.OUTGOING_CALL_CALLER_ID,
                callerName
            )
            return intent
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DataBindingUtil.setContentView<ActivityCallBinding>(
            this,
            R.layout.activity_call
        )
            .also { binding = it }
            .apply {
//                webexViewModel.callObserverInterface = this@CallActivity
                reload()

            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pictureInPictureParamsBuilder = PictureInPictureParams.Builder()
        }


    }

    fun reload() {

        val fragment = addNewFragment()
        Handler(Looper.getMainLooper()).postDelayed({
            val callerId =
                intent.getStringExtra(Constants.Intent.OUTGOING_CALL_CALLER_ID)

            callerId?.let {
                fragment.dialOutgoingCall(
                    callerId
                )
            }
        }, 100)
    }
    private fun getLastFragment(): CallControlsFragment? {
        for (fragment in supportFragmentManager.fragments) {
            if (fragment is CallControlsFragment) {
                return fragment
            }
        }
        return null
    }


    private fun addNewFragment(): CallControlsFragment {
        val callId =
            intent?.getStringExtra(Constants.Intent.CALL_ID) ?: "default"
        var transaction = supportFragmentManager.beginTransaction()
        val newCallControlFragment = CallControlsFragment()
        newCallControlFragment.arguments = intent?.extras
        transaction.replace(
            R.id.fragment_container_view,
            newCallControlFragment,
            "call-" + callId
        )
        transaction.commit()
        return newCallControlFragment
    }


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)

    }


    override fun onBackPressed() {
        val fragment = getLastFragment()
        fragment?.let {
            fragment.onBackPressed()
        }
        super.onBackPressed()
    }

    fun alertDialog(shouldFinishActivity: Boolean, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(resources.getString(R.string.call_failed))
        builder.setMessage(message)

        builder.setPositiveButton("OK") { _, _ ->


            reload()
        }

        builder.show()
    }

    private fun toBeShownOnLockScreen() {
        window.addFlags(
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        toBeShownOnLockScreen()
    }

    private fun pictureInPictureMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val fragment = getLastFragment()
            fragment?.let {
                val aspectRatio = fragment.aspectRatio()
                pictureInPictureParamsBuilder?.setAspectRatio(aspectRatio)
                    ?.build()
                pictureInPictureParamsBuilder?.build()
                    ?.let { enterPictureInPictureMode(it) }
            }
        } else {
            Toast.makeText(
                this,
                "Your device doesn't support PIP",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pictureInPictureMode()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        val fragment = getLastFragment()
        fragment?.let {
            if (isInPictureInPictureMode) {
                fragment.pipVisibility(View.GONE, isInPictureInPictureMode)
            } else {
                fragment.pipVisibility(View.VISIBLE, isInPictureInPictureMode)
            }
        }
    }


    override fun finish() {
        if (calls.isNotEmpty()) {
            //Resume a queued call
            var resumedCall = calls.get(0)
            var transaction = supportFragmentManager.beginTransaction()
            val newCallControlFragment = CallControlsFragment()
            newCallControlFragment.arguments =
                argumentList[resumedCall.getCallId()]
            transaction.replace(
                R.id.fragment_container_view,
                newCallControlFragment,
                "call-" + resumedCall.getCallId()!!
            )
            transaction.commit()
            calls.remove(resumedCall)
        } else {
            super.finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webexViewModel.cleanup()
    }

}