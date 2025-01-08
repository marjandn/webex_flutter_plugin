package marjandn.webex_flutter_plugin

import marjandn.webex_flutter_plugin.calling.CallObserverInterface
import android.util.Log
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.ciscowebex.androidsdk.Webex

import marjandn.webex_flutter_plugin.utils.CallObjectStorage
import com.ciscowebex.androidsdk.CompletionHandler
import com.ciscowebex.androidsdk.phone.BreakoutSession.BreakoutSessionError
import com.ciscowebex.androidsdk.phone.Call
import com.ciscowebex.androidsdk.phone.CallObserver
import com.ciscowebex.androidsdk.phone.MediaOption
import com.ciscowebex.androidsdk.phone.Phone
import com.ciscowebex.androidsdk.phone.AdvancedSetting
import com.ciscowebex.androidsdk.phone.MediaStream
import com.ciscowebex.androidsdk.phone.BreakoutSession
import com.ciscowebex.androidsdk.phone.Breakout
import com.ciscowebex.androidsdk.phone.ReceivingNoiseInfo
import com.ciscowebex.androidsdk.phone.closedCaptions.CaptionItem
import com.ciscowebex.androidsdk.phone.closedCaptions.ClosedCaptionsInfo
import kotlin.collections.get

class WebexViewModel(val webex: Webex, val repository: WebexRepository) :
    BaseViewModel() {
    private val tag = "WebexViewModel"


    private val _authLiveData = MutableLiveData<String>()
    val _callingLiveData = MutableLiveData<WebexRepository.CallLiveData>()


    val _setCompositeLayoutLiveData = MutableLiveData<Pair<Boolean, String>>()

    val _forceSendingVideoLandscapeLiveData = MutableLiveData<Boolean>()


    val authLiveData: LiveData<String> = _authLiveData
    val callingLiveData: LiveData<WebexRepository.CallLiveData> =
        _callingLiveData


    val setCompositeLayoutLiveData: LiveData<Pair<Boolean, String>> =
        _setCompositeLayoutLiveData

    val forceSendingVideoLandscapeLiveData: LiveData<Boolean> =
        _forceSendingVideoLandscapeLiveData


    private val _virtualBgError = MutableLiveData<String>()
    val virtualBgError: LiveData<String> = _virtualBgError


    private val _annotationEvent = MutableLiveData<AnnotationEvent>()
    val annotationEvent: LiveData<AnnotationEvent> get() = _annotationEvent

    sealed class AnnotationEvent {
        data class PERMISSION_ASK(val personId: String) : AnnotationEvent()
        data class PERMISSION_EXPIRED(val personId: String) : AnnotationEvent()
    }



    var compositedLayoutState = MediaOption.CompositedVideoLayout.NOT_SUPPORTED

    var callObserverInterface: CallObserverInterface? = null

    var isSendingVideoForceLandscape: Boolean = false

    var callCapability: WebexRepository.CallCap
        get() = repository.callCapability
        set(value) {
            repository.callCapability = value
        }


    var compositedVideoLayout: MediaOption.CompositedVideoLayout
        get() = repository.compositedVideoLayout
        set(value) {
            repository.compositedVideoLayout = value
        }

    var streamMode: Phone.VideoStreamMode
        get() = repository.streamMode
        set(value) {
            repository.streamMode = value
        }

    var isAddedCall: Boolean
        get() = repository.isAddedCall
        set(value) {
            repository.isAddedCall = value
        }

    var currentCallId: String?
        get() = repository.currentCallId
        set(value) {
            repository.currentCallId = value
        }

    var oldCallId: String?
        get() = repository.oldCallId
        set(value) {
            repository.oldCallId = value
        }

    var isLocalVideoMuted: Boolean
        get() = repository.isLocalVideoMuted
        set(value) {
            repository.isLocalVideoMuted = value
        }

    var isRemoteVideoMuted: Boolean
        get() = repository.isRemoteVideoMuted
        set(value) {
            repository.isRemoteVideoMuted = value
        }


    var isRemoteScreenShareON: Boolean
        get() = repository.isRemoteScreenShareON
        set(value) {
            repository.isRemoteScreenShareON = value
        }

    var enableBgStreamtoggle: Boolean
        get() = repository.enableBgStreamtoggle
        set(value) {
            repository.enableBgStreamtoggle = value
        }

    var enableBgConnectiontoggle: Boolean
        get() = repository.enableBgConnectiontoggle
        set(value) {
            repository.enableBgConnectiontoggle = value
        }

    init {
        repository._authLiveDataList.add(_authLiveData)
    }


    override fun onCleared() {
        repository.clearCallData()
        repository._authLiveDataList.remove(_authLiveData)
    }

    fun dial(input: String, option: MediaOption) {
        webex.phone.dial(input, option, CompletionHandler { result ->
            Log.d(tag, "dial isSuccessful: ${result.isSuccessful}")
            if (result.isSuccessful) {
                result.data?.let { _call ->
                    CallObjectStorage.addCallObject(_call)
                    currentCallId = _call.getCallId()
                    setCallObserver(_call)
                    _callingLiveData.postValue(
                        WebexRepository.CallLiveData(
                            WebexRepository.CallEvent.DialCompleted, _call
                        )
                    )
                }
            } else {
                result.error?.let { error ->

                    _callingLiveData.postValue(
                        WebexRepository.CallLiveData(
                            WebexRepository.CallEvent.DialFailed,
                            null,
                            null,
                            result.error?.errorMessage
                        )
                    )
                } ?: run {
                    _callingLiveData.postValue(
                        WebexRepository.CallLiveData(
                            WebexRepository.CallEvent.DialFailed,
                            null,
                            null,
                            result.error?.errorMessage
                        )
                    )
                }
            }
        })
    }


    inner class VMCallObserver(val call: Call) : CallObserver {
        override fun onConnected(call: Call?) {
            Log.d(tag, "CallObserver onConnected")
            callObserverInterface?.onConnected(call)
        }

        override fun onRinging(call: Call?) {
            Log.d(tag, "CallObserver onRinging")
            callObserverInterface?.onRinging(call)
        }

        override fun onStartRinging(call: Call?, ringerType: Call.RingerType) {
            Log.d(tag, "CallObserver onStartRinging")
            callObserverInterface?.onStartRinging(call, ringerType)
        }

        override fun onStopRinging(call: Call?, ringerType: Call.RingerType) {
            Log.d(tag, "CallObserver onStopRinging")
            callObserverInterface?.onStopRinging(call, ringerType)
        }

        override fun onWaiting(call: Call?, reason: Call.WaitReason?) {
            Log.d(tag, "CallObserver onWaiting reason: $reason")
            callObserverInterface?.onWaiting(call)
        }

        override fun onDisconnected(event: CallObserver.CallDisconnectedEvent?) {
            Log.d(
                tag,
                "CallObserver onDisconnected event: ${this@WebexViewModel} $callObserverInterface $event"
            )
            callObserverInterface?.onDisconnected(call, event)
        }

        override fun onInfoChanged(call: Call?) {
            callObserverInterface?.onInfoChanged(call)
        }

        override fun onMediaChanged(event: CallObserver.MediaChangedEvent?) {
            Log.d(tag, "CallObserver OnMediaChanged event: $event")
            callObserverInterface?.onMediaChanged(call, event)
            event?.getCall()
                ?.let {
                    CallObjectStorage.updateCallObject(
                        call.getCallId().toString(), it
                    )
                }

        }

        override fun onCallMembershipChanged(event: CallObserver.CallMembershipChangedEvent?) {
            Log.d(tag, "CallObserver onCallMembershipChanged event: $event")
            callObserverInterface?.onCallMembershipChanged(call, event)
            getParticipants(event?.getCall()?.getCallId().orEmpty())
        }

        override fun onScheduleChanged(call: Call?) {
            callObserverInterface?.onScheduleChanged(call)
        }

        override fun onCpuHitThreshold() {
            callObserverInterface?.onCpuHitThreshold()
        }

        override fun onPhotoCaptured(imageData: ByteArray?) {
            callObserverInterface?.onPhotoCaptured(imageData)
        }

        override fun onMediaQualityInfoChanged(mediaQualityInfo: Call.MediaQualityInfo) {
            callObserverInterface?.onMediaQualityInfoChanged(mediaQualityInfo)
        }

        override fun onBroadcastMessageReceivedFromHost(message: String) {
            callObserverInterface?.onBroadcastMessageReceivedFromHost(message)
        }

        override fun onHostAskingReturnToMainSession() {
            callObserverInterface?.onHostAskingReturnToMainSession()
        }

        override fun onJoinableSessionUpdated(breakoutSessions: List<BreakoutSession>) {
            callObserverInterface?.onJoinableSessionUpdated(breakoutSessions)
        }

        override fun onJoinedSessionUpdated(breakoutSession: BreakoutSession) {
            callObserverInterface?.onJoinedSessionUpdated(breakoutSession)
        }

        override fun onReturnedToMainSession() {
            callObserverInterface?.onReturnedToMainSession()
        }

        override fun onSessionClosing() {
            callObserverInterface?.onSessionClosing()
        }

        override fun onSessionEnabled() {
            callObserverInterface?.onSessionEnabled()
        }

        override fun onSessionJoined(breakoutSession: BreakoutSession) {
            callObserverInterface?.onSessionJoined(breakoutSession)
        }

        override fun onSessionStarted(breakout: Breakout) {
            callObserverInterface?.onSessionStarted(breakout)
        }

        override fun onBreakoutUpdated(breakout: Breakout) {
            callObserverInterface?.onBreakoutUpdated(breakout)
        }

        override fun onBreakoutError(error: BreakoutSessionError) {
            callObserverInterface?.onBreakoutError(error)
        }

        override fun onReceivingNoiseInfoChanged(info: ReceivingNoiseInfo) {
            callObserverInterface?.onReceivingNoiseInfoChanged(info)
        }

        override fun onClosedCaptionsArrived(closedCaptions: CaptionItem) {
            callObserverInterface?.onClosedCaptionsArrived(closedCaptions)
        }

        override fun onClosedCaptionsInfoChanged(closedCaptionsInfo: ClosedCaptionsInfo) {
            callObserverInterface?.onClosedCaptionsInfoChanged(
                closedCaptionsInfo
            )
        }
    }

    val callObserverMap: HashMap<String, VMCallObserver> = HashMap()

    fun setCallObserver(call: Call) {

        var observer = callObserverMap[call.getCallId()]
        if (observer == null) {
            observer = VMCallObserver(call)
            callObserverMap[call.getCallId()!!] = observer
        }
        repository.setCallObserver(call, observer)
    }

    fun getCall(callId: String): Call? {
        return repository.getCall(callId)
    }

    fun handleAnnotationPermission(grant: Boolean, personId: String) {
        getCall(currentCallId.orEmpty())?.getLiveAnnotationHandle()
            ?.respondToLiveAnnotationRequest(personId, grant) {
                if (it.isSuccessful) {
                    Log.d(tag, "permission handled")
                } else {
                    Log.d(tag, "permission error: ${it.error?.errorMessage}")
                }
            }
    }

    fun cancel() {
        webex.phone.cancel()
    }

    fun hangup(callId: String) {
        getCall(callId)?.hangup(CompletionHandler { result ->
            if (result.isSuccessful) {
                Log.d(tag, "hangup successful")
            } else {
                Log.d(tag, "hangup error: ${result.error?.errorMessage}")
            }
        })
    }

    fun holdCall(callId: String) {
        val callInfo = getCall(callId)
        val isOnHold = callInfo?.isOnHold() ?: false
        Log.d(tag, "holdCall isOnHold = $isOnHold")
        callInfo?.holdCall(!isOnHold)
    }


    fun getParticipants(_callId: String) {
        val callParticipants = getCall(_callId)?.getMemberships() ?: ArrayList()


        callParticipants.forEach {
            repository.participantMuteMap[it.getPersonId()] =
                it.isSendingAudio()
        }
    }

    fun setVideoMaxTxFPSSetting(fps: Int) {
        webex.phone.setAdvancedSetting(AdvancedSetting.VideoMaxTxFPS(fps) as AdvancedSetting<*>)
    }

    fun setVideoEnableDecoderMosaicSetting(value: Boolean) {
        webex.phone.setAdvancedSetting(
            AdvancedSetting.VideoEnableDecoderMosaic(
                value
            ) as AdvancedSetting<*>
        )
    }

    fun setShareMaxCaptureFPSSetting(fps: Int) {
        webex.phone.setAdvancedSetting(AdvancedSetting.ShareMaxCaptureFPS(fps) as AdvancedSetting<*>)
    }

    fun setVideoEnableCamera2Setting(value: Boolean) {
        webex.phone.setAdvancedSetting(AdvancedSetting.VideoEnableCamera2(value) as AdvancedSetting<*>)
    }

    fun enableAudioBNR(value: Boolean) {
        webex.phone.enableAudioBNR(value)
    }

    fun setAudioBNRMode(mode: Phone.AudioBRNMode) {
        webex.phone.setAudioBNRMode(mode)
    }

    fun setDefaultFacingMode(mode: Phone.FacingMode) {
        webex.phone.setDefaultFacingMode(mode)
    }


    fun setSharingMaxRxBandwidth(bandwidth: Int) {
        webex.phone.setSharingMaxRxBandwidth(bandwidth)
    }

    fun setAudioMaxRxBandwidth(bandwidth: Int) {
        webex.phone.setAudioMaxRxBandwidth(bandwidth)
    }


    fun enableBackgroundConnection(enable: Boolean) {
        webex.phone.enableBackgroundConnection(enable)
    }

    fun enableBackgroundStream(enable: Boolean) {
        webex.phone.enableBackgroundStream(enable)
    }

    fun getVideoRenderViews(callId: String): Pair<View?, View?> {
        return getCall(callId)?.getVideoRenderViews() ?: Pair(null, null)
    }

    fun setVideoRenderViews(
        callId: String,
        localVideoView: View?,
        remoteVideoView: View?
    ) {
        getCall(callId)?.setVideoRenderViews(
            Pair(
                localVideoView,
                remoteVideoView
            )
        )
    }

    fun setVideoRenderViews(callId: String) {
        getCall(callId)?.setVideoRenderViews(null)
    }

    fun setVideoStreamMode(mode: Phone.VideoStreamMode) {
        webex.phone.setVideoStreamMode(mode)
    }

    fun getCompositedLayout(): MediaOption.CompositedVideoLayout {
        return getCall(currentCallId.orEmpty())?.getCompositedVideoLayout()
            ?: MediaOption.CompositedVideoLayout.NOT_SUPPORTED
    }

    fun setCompositedLayout(compositedLayout: MediaOption.CompositedVideoLayout) {
        compositedLayoutState = compositedLayout
        getCall(currentCallId.orEmpty())?.setCompositedVideoLayout(
            compositedLayout,
            CompletionHandler { result ->
                if (result.isSuccessful) {
                    Log.d(tag, "setCompositedLayout Lambda isSuccessful")
                    _setCompositeLayoutLiveData.postValue(Pair(true, ""))
                } else {
                    Log.d(
                        tag,
                        "setCompositedLayout Lambda error: ${result.error?.errorMessage}"
                    )
                    _setCompositeLayoutLiveData.postValue(
                        Pair(
                            false,
                            result.error?.errorMessage ?: ""
                        )
                    )
                }
            })
    }

    fun getMediaStreams(): List<MediaStream>? {
        return getCall(currentCallId.orEmpty())?.getMediaStreams()
    }

    fun cleanup() {

        for (entry in callObserverMap.entries.iterator()) {
            repository.removeCallObserver(entry.key, entry.value)
        }
        callObserverMap.clear()
    }


    fun clearCallObservers(callId: String) {
        repository.clearCallObservers(callId)
    }

    fun enableStreams() {
        webex.phone.enableStreams(true)
    }

}