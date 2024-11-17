package ae.altkamul.webex_flutter_plugin

import ae.altkamul.webex_flutter_plugin.utils.CallObjectStorage
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.ciscowebex.androidsdk.Webex
import com.ciscowebex.androidsdk.WebexUCLoginDelegate
import com.ciscowebex.androidsdk.message.Message
import com.ciscowebex.androidsdk.people.Person
import com.ciscowebex.androidsdk.space.Space
import com.ciscowebex.androidsdk.CompletionHandler
import com.ciscowebex.androidsdk.WebexAuthDelegate
import com.ciscowebex.androidsdk.auth.PhoneServiceRegistrationFailureReason
import com.ciscowebex.androidsdk.auth.UCLoginFailureReason
import com.ciscowebex.androidsdk.auth.UCLoginServerConnectionStatus
import com.ciscowebex.androidsdk.auth.UCSSOFailureReason
import com.ciscowebex.androidsdk.calendarMeeting.CalendarMeetingObserver
import com.ciscowebex.androidsdk.membership.Membership
import com.ciscowebex.androidsdk.membership.MembershipObserver
import com.ciscowebex.androidsdk.message.LocalFile
import com.ciscowebex.androidsdk.message.MessageObserver
import com.ciscowebex.androidsdk.phone.Breakout
import com.ciscowebex.androidsdk.phone.BreakoutSession
import com.ciscowebex.androidsdk.phone.Call
import com.ciscowebex.androidsdk.phone.CallMembership
import com.ciscowebex.androidsdk.phone.MediaOption
import com.ciscowebex.androidsdk.phone.Phone
import com.ciscowebex.androidsdk.phone.VirtualBackground
import com.ciscowebex.androidsdk.phone.CallObserver
import com.ciscowebex.androidsdk.phone.NotificationCallType
import com.ciscowebex.androidsdk.phone.ReceivingNoiseInfo
import com.ciscowebex.androidsdk.phone.closedCaptions.CaptionItem
import com.ciscowebex.androidsdk.phone.closedCaptions.ClosedCaptionsInfo
import com.ciscowebex.androidsdk.space.SpaceObserver
import java.io.PrintWriter

class WebexRepository(val webex: Webex) : WebexUCLoginDelegate, WebexAuthDelegate {
    private val tag = "WebexRepository"

    enum class CallCap {
        Audio_Only,
        Audio_Video
    }
    enum class CallEvent {
        DialCompleted,
        DialFailed,
        AnswerCompleted,
        AnswerFailed,
        AssociationCallCompleted,
        AssociationCallFailed,
        MeetingPinOrPasswordRequired,
        CaptchaRequired,
        InCorrectPassword,
        InCorrectPasswordWithCaptcha,
        InCorrectPasswordOrHostKey,
        InCorrectPasswordOrHostKeyWithCaptcha,
        WrongApiCalled,
        CannotStartInstantMeeting
    }


    data class CallLiveData(val event: CallEvent,
                            val call: Call? = null,
                            val captcha: Phone.Captcha? = null,
                            val sharingLabel: String? = null,
                            val errorMessage: String? = null,
                            val callMembershipEvent: CallObserver.CallMembershipChangedEvent? = null,
                            val mediaChangeEvent: CallObserver.MediaChangedEvent? = null,
                            val disconnectEvent: CallObserver.CallDisconnectedEvent? = null) {}

    var isAddedCall = false
    var currentCallId: String? = null
    var oldCallId: String? = null
    var isSendingAudio = true
    var doMuteAll = true
    var isLocalVideoMuted = true
    var isRemoteVideoMuted = true
    var isRemoteScreenShareON = false
    var enableBgStreamtoggle = true
    var enableBgConnectiontoggle = true

    var callCapability: CallCap = CallCap.Audio_Video
    var compositedVideoLayout: MediaOption.CompositedVideoLayout = MediaOption.CompositedVideoLayout.FILMSTRIP
    var streamMode: Phone.VideoStreamMode = Phone.VideoStreamMode.AUXILIARY

    val participantMuteMap = hashMapOf<String, Boolean>()

    var _authLiveDataList: MutableList<MutableLiveData<String>?> = mutableListOf()

    var _callObservers : HashMap<String, MutableList<CallObserver>> = HashMap()

    init {
        webex.delegate = this
        webex.authDelegate = this
    }

    fun clearCallData() {
        isAddedCall = false
        currentCallId = null
        oldCallId = null
        isSendingAudio = true
        doMuteAll = true
        isLocalVideoMuted = true
        isRemoteScreenShareON = false
        isRemoteVideoMuted = true

    }

    fun getCall(callId: String): Call? {
        return CallObjectStorage.getCallObject(callId)
    }

    @Synchronized
    fun setCallObserver(call: Call, callObserver: CallObserver){
        val callId = call.getCallId() ?: return
        var observers = _callObservers[callId]
        var registerFirstTime = false
        if(observers == null){
            registerFirstTime = true
            observers = mutableListOf()
        }
        if(!observers.contains(callObserver)) {
            observers.add(callObserver)
        }
        _callObservers[callId] = observers
        if(registerFirstTime)
            registerCallObserver(call)
    }

    inner class WxCallObserver(private val _callId : String) : CallObserver {
        override fun onWaiting(call: Call?, reason: Call.WaitReason?) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onWaiting(call, reason)
                }
            }
        }

        override fun onScheduleChanged(call: Call?) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onScheduleChanged(call)
                }
            }
        }

        override fun onRinging(call: Call?) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onRinging(call)
                }
            }
        }

        override fun onStopRinging(call: Call?, ringerType: Call.RingerType) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onStopRinging(call, ringerType)
                }
            }
        }

        override fun onStartRinging(call: Call?, ringerType: Call.RingerType) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    Log.d(tag, "start ringer repository")
                    observer.onStartRinging(call, ringerType)
                }
            }
        }

        override fun onConnected(call: Call?) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onConnected(call)
                }
            }
        }

        override fun onDisconnected(event: CallObserver.CallDisconnectedEvent?) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onDisconnected(event)
                }
            }
            CallObjectStorage.removeCallObject(_callId)
        }

        override fun onInfoChanged(call: Call?) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onInfoChanged(call)
                }
            }
        }

        override fun onCallMembershipChanged(event: CallObserver.CallMembershipChangedEvent?) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onCallMembershipChanged(event)
                }
            }
        }

        override fun onCpuHitThreshold() {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onCpuHitThreshold()
                }
            }
        }

        override fun onPhotoCaptured(imageData: ByteArray?) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onPhotoCaptured(imageData)
                }
            }
        }

        override fun onMediaQualityInfoChanged(mediaQualityInfo: Call.MediaQualityInfo) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onMediaQualityInfoChanged(mediaQualityInfo)
                }
            }
        }

        override fun onSessionEnabled() {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onSessionEnabled()
                }
            }
        }

        override fun onSessionStarted(breakout: Breakout) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onSessionStarted(breakout)
                }
            }
        }

        override fun onBreakoutUpdated(breakout: Breakout) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onBreakoutUpdated(breakout)
                }
            }
        }

        override fun onSessionJoined(breakoutSession: BreakoutSession) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onSessionJoined(breakoutSession)
                }
            }
        }

        override fun onJoinedSessionUpdated(breakoutSession: BreakoutSession) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onJoinedSessionUpdated(breakoutSession)
                }
            }
        }

        override fun onJoinableSessionUpdated(breakoutSessions: List<BreakoutSession>) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onJoinableSessionUpdated(breakoutSessions)
                }
            }
        }

        override fun onHostAskingReturnToMainSession() {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onHostAskingReturnToMainSession()
                }
            }
        }

        override fun onBroadcastMessageReceivedFromHost(message: String) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onBroadcastMessageReceivedFromHost(message)
                }
            }
        }

        override fun onSessionClosing() {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onSessionClosing()
                }
            }
        }

        override fun onReturnedToMainSession() {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onReturnedToMainSession()
                }
            }
        }

        override fun onBreakoutError(error: BreakoutSession.BreakoutSessionError) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onBreakoutError(error)
                }
            }
        }

        override fun onMediaChanged(event: CallObserver.MediaChangedEvent?) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onMediaChanged(event)
                }
            }
        }

        override fun onReceivingNoiseInfoChanged(info: ReceivingNoiseInfo) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onReceivingNoiseInfoChanged(info)
                }
            }
        }

        override fun onClosedCaptionsArrived(closedCaptions: CaptionItem) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onClosedCaptionsArrived(closedCaptions)
                }
            }
        }

        override fun onClosedCaptionsInfoChanged(closedCaptionsInfo: ClosedCaptionsInfo) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onClosedCaptionsInfoChanged(closedCaptionsInfo)
                }
            }
        }

        override fun onMoveMeetingFailed(call: Call?) {
            val observers: MutableList<CallObserver>? = _callObservers[_callId]
            observers?.let { it ->
                it.forEach { observer ->
                    observer.onMoveMeetingFailed(call)
                }
            }
        }
    }

    private fun registerCallObserver(call: Call) {
        call.getCallId()?.let {
            call.setObserver(WxCallObserver(it))
        }
    }

    fun removeCallObserver(callId : String, observer: CallObserver){
        var observers = _callObservers[callId]
        observers?.let{
            observers.remove(observer)
            if(it.size == 0)
                _callObservers.remove(callId)
        }
    }

    fun clearCallObservers(callId: String) {
        _callObservers.remove(callId)
    }
}