package ae.altkamul.webex_flutter_plugin

import ae.altkamul.webex_flutter_plugin.auth.JWTLoginActivity
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Pair
import android.util.Rational
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.ciscowebex.androidsdk.WebexError
import ae.altkamul.webex_flutter_plugin.databinding.FragmentCallControlsBinding
import ae.altkamul.webex_flutter_plugin.utils.CallObjectStorage
import ae.altkamul.webex_flutter_plugin.utils.showDialogWithMessage
import ae.altkamul.webex_flutter_plugin.utils.UIUtils
import com.ciscowebex.androidsdk.phone.Call
import com.ciscowebex.androidsdk.phone.CallObserver
import com.ciscowebex.androidsdk.phone.MediaOption
import com.ciscowebex.androidsdk.phone.MediaRenderView
import com.ciscowebex.androidsdk.phone.MediaStreamChangeEventType
import com.ciscowebex.androidsdk.phone.MediaStreamChangeEventInfo
import com.ciscowebex.androidsdk.phone.MediaStreamType
import com.ciscowebex.androidsdk.phone.Phone
import com.ciscowebex.androidsdk.phone.Breakout
import com.ciscowebex.androidsdk.phone.BreakoutSession
import com.ciscowebex.androidsdk.phone.CompanionMode
import com.ciscowebex.androidsdk.phone.ReceivingNoiseInfo
import com.ciscowebex.androidsdk.phone.RemoteShareCallback
import com.ciscowebex.androidsdk.phone.closedCaptions.CaptionItem
import com.ciscowebex.androidsdk.phone.closedCaptions.ClosedCaptionsInfo
import ae.altkamul.webex_flutter_plugin.utils.GlobalExceptionHandler
import ae.altkamul.webex_flutter_plugin.calling.CallActivity
import ae.altkamul.webex_flutter_plugin.calling.CallObserverInterface
import android.widget.ImageButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class CallControlsFragment : Fragment(), OnClickListener, CallObserverInterface,
    RemoteShareCallback {
    val webexViewModel: WebexViewModel by viewModel()

    private lateinit var binding: FragmentCallControlsBinding
    private var callFailed = false


    private val mediaPlayer: MediaPlayer = MediaPlayer()

    private var isInPipMode = false

    private lateinit var annotationPermissionDialog: AlertDialog

    private val mHandler = Handler(Looper.getMainLooper())
    private var callManagementServiceIntent: Intent? = null


    enum class NetworkStatus {
        PoorUplink,
        PoorDownlink,
        Good,
        NoNetwork
    }

    private val mAuxStreamViewMap: HashMap<View, AuxStreamViewHolder> =
        HashMap()
    var currentNetworkStatus = NetworkStatus.Good

    class AuxStreamViewHolder(var item: View) {
        var mediaRenderView: MediaRenderView =
            item.findViewById(R.id.view_video)
        var textView: TextView = item.findViewById(R.id.name)
        var audioState: ImageView = item.findViewById(R.id.iv_audio_state)
        var viewAvatar: ImageView = item.findViewById(R.id.view_avatar)
        var remoteBorder: RelativeLayout = item.findViewById(R.id.remote_border)
        var moreOption: ImageButton = item.findViewById(R.id.ib_more_option)
        var streamType: MediaStreamType = MediaStreamType.Unknown
        var parentLayout: RelativeLayout = item.findViewById(R.id.parentLayout)
        var pinStreamImageView: ImageView = item.findViewById(R.id.iv_pinstream)
        var personID: String? = null
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return DataBindingUtil.inflate<FragmentCallControlsBinding>(
            LayoutInflater.from(context),
            R.layout.fragment_call_controls, container, false
        ).also { binding = it }.apply {

            setUpViews(getArguments())
            observerCallLiveData()

        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.screenShareView.setRemoteShareCallback(this)
    }

    private fun mediaStreamInfoChangedListener(
        type: MediaStreamChangeEventType,
        info: MediaStreamChangeEventInfo
    ) {
        mHandler.post {
            Log.d(
                tag,
                "CallObserver OnMediaChanged setOnMediaStreamInfoChanged type: $type  name: ${
                    info.getStream().getPerson()?.getDisplayName()
                }"
            )
            when (type) {
                MediaStreamChangeEventType.Video -> {
                    val auxStreamViewHolder =
                        mAuxStreamViewMap[info.getStream().getRenderView()]

                    if (auxStreamViewHolder != null) {
                        Log.d(
                            tag,
                            "CallObserver OnMediaChanged setOnMediaStreamInfoChanged isSendingVideo: ${
                                info.getStream().getPerson()?.isSendingVideo()
                            }"
                        )
                        if (info.getStream().getPerson()
                                ?.isSendingVideo() == true
                        ) {
                            auxStreamViewHolder.viewAvatar.visibility =
                                View.GONE
                            auxStreamViewHolder.mediaRenderView.visibility =
                                View.VISIBLE
                            info.getStream()
                                .setRenderView(auxStreamViewHolder.mediaRenderView)
                        } else {
                            val membership = info.getStream().getPerson()
                            membership?.let { member ->
                                if (member.getPersonId().isNotEmpty()) {
                                    Log.d(
                                        tag,
                                        "CallObserver OnMediaChanged setOnMediaStreamInfoChanged viewAvatar visible"
                                    )
                                    auxStreamViewHolder.viewAvatar.visibility =
                                        View.VISIBLE
                                    auxStreamViewHolder.mediaRenderView.visibility =
                                        View.GONE
                                }
                            }
                        }
                    }
                }

                MediaStreamChangeEventType.Audio -> {
                    val auxStreamViewHolder =
                        mAuxStreamViewMap[info.getStream().getRenderView()]

                    if (auxStreamViewHolder != null) {
                        Log.d(
                            tag,
                            "CallObserver OnMediaChanged setOnMediaStreamInfoChanged isSendingAudio: " + info.getStream()
                                .getPerson()?.isSendingAudio()
                        )
                        val membership = info.getStream().getPerson()
                        membership?.let { member ->
                            if (member.getPersonId().isNotEmpty()) {
                                if (member.isSendingAudio()) {
                                    auxStreamViewHolder.audioState.setImageResource(
                                        R.drawable.ic_microphone_36
                                    )
                                } else {
                                    auxStreamViewHolder.audioState.setImageResource(
                                        R.drawable.ic_microphone_muted_bold
                                    )
                                }
                            }
                        }
                    } else {
                        if (info.getStream()
                                .getStreamType() == MediaStreamType.Stream1
                        ) {
                            setRemoteVideoInformation(
                                info.getStream().getPerson()?.getDisplayName()
                                    .orEmpty(),
                                !(info.getStream().getPerson()?.isSendingAudio()
                                    ?: true)
                            )
                        }
                    }
                }

                MediaStreamChangeEventType.Size -> {
                    Log.d(
                        tag,
                        "CallObserver OnMediaChanged setOnMediaStreamInfoChanged width: " + info.getStream()
                            .getSize().width +
                                " height: " + info.getStream().getSize().height
                    )
                }

                MediaStreamChangeEventType.PinState -> {
                    val auxStreamViewHolder =
                        mAuxStreamViewMap[info.getStream().getRenderView()]

                    if (auxStreamViewHolder != null) {
                        Log.d(
                            tag,
                            "CallObserver OnMediaChanged setOnMediaStreamInfoChanged PinState " +
                                    "isPinned: ${
                                        info.getStream().isPinned()
                                    } personID: ${
                                        info.getStream().getPerson()
                                            ?.getPersonId()
                                    }"
                        )
                        val membership = info.getStream().getPerson()
                        membership?.let { member ->
                            if (member.getPersonId().isNotEmpty()) {
                                Log.d(
                                    tag,
                                    "CallObserver OnMediaChanged setOnMediaStreamInfoChanged PinState getPersonId not empty"
                                )
                                if (isMediaStreamAlreadyPinned(
                                        member.getPersonId(),
                                        info.getStream().getStreamType()
                                    )
                                ) {
                                    Log.d(
                                        tag,
                                        "CallObserver OnMediaChanged setOnMediaStreamInfoChanged PinState isPinned"
                                    )
                                    auxStreamViewHolder.pinStreamImageView.visibility =
                                        View.VISIBLE
                                    auxStreamViewHolder.parentLayout.background =
                                        ContextCompat.getDrawable(
                                            requireActivity(),
                                            R.drawable.border_category_c
                                        )
                                } else {
                                    auxStreamViewHolder.pinStreamImageView.visibility =
                                        View.GONE
                                    auxStreamViewHolder.parentLayout.background =
                                        ContextCompat.getDrawable(
                                            requireActivity(),
                                            R.drawable.border_category_b
                                        )
                                }
                            }
                        }
                    }
                }

                MediaStreamChangeEventType.Membership -> {
                    Log.d(
                        tag,
                        "CallObserver OnMediaChanged setOnMediaStreamInfoChanged Membership from: ${
                            info.fromMembership().getPersonId()
                        } to: ${info.toMembership().getPersonId()}"
                    )
                    val auxStreamViewHolder =
                        mAuxStreamViewMap[info.getStream().getRenderView()]
                    val membership = info.getStream().getPerson()
                    membership?.let { member ->
                        Log.d(
                            tag,
                            "CallObserver OnMediaChanged setOnMediaStreamInfoChanged name: " + member.getDisplayName()
                        )
                        auxStreamViewHolder?.viewAvatar?.visibility =
                            if (member.isSendingVideo()) View.GONE else View.VISIBLE
                        auxStreamViewHolder?.textView?.text =
                            member.getDisplayName()
                        auxStreamViewHolder?.personID = member.getPersonId()
                        auxStreamViewHolder?.streamType =
                            info.getStream().getStreamType()
                        if (isMediaStreamAlreadyPinned(
                                member.getPersonId(),
                                auxStreamViewHolder?.streamType
                            )
                        ) {
                            auxStreamViewHolder?.pinStreamImageView?.visibility =
                                View.VISIBLE
                            auxStreamViewHolder?.parentLayout?.background =
                                ContextCompat.getDrawable(
                                    requireActivity(),
                                    R.drawable.border_category_c
                                )
                        } else {
                            auxStreamViewHolder?.pinStreamImageView?.visibility =
                                View.GONE
                            auxStreamViewHolder?.parentLayout?.background =
                                ContextCompat.getDrawable(
                                    requireActivity(),
                                    R.drawable.border_category_b
                                )
                        }

                        if (info.getStream()
                                .getStreamType() == MediaStreamType.Stream1
                        ) {
                            setRemoteVideoInformation(
                                member.getDisplayName().orEmpty(),
                                !(member.isSendingAudio())
                            )
                        }
                    }
                }

                else -> {}
            }
        }
    }

    override fun onResume() {
        super.onResume()

        webexViewModel.currentCallId?.let {
            onVideoStreamingChanged(it)
        }
        webexViewModel.enableStreams()
    }

    private fun getMediaOption(
        isModerator: Boolean = false,
        pin: String = "",
        captcha: String = "",
        captchaId: String = "",
        companionMode: CompanionMode = CompanionMode.None
    ): MediaOption {
        val mediaOption: MediaOption =
            if (webexViewModel.callCapability == WebexRepository.CallCap.Audio_Only) {
                MediaOption.audioOnly()
            } else {
                MediaOption.audioVideoSharing(
                    Pair(
                        binding.localView,
                        binding.remoteView
                    ), binding.screenShareView
                )
            }
        mediaOption.setModerator(isModerator)
        mediaOption.setPin(pin)
        mediaOption.setCaptchaCode(captcha)
        mediaOption.setCaptchaId(captchaId)
        mediaOption.setCompanionMode(companionMode)
        return mediaOption
    }

    fun dialOutgoingCall(
        callerId: String,
        isModerator: Boolean = false,
        pin: String = "",
        captcha: String = "",
        captchaId: String = "",
        moveMeeting: CompanionMode = CompanionMode.None
    ) {

        webexViewModel.dial(
            callerId,
            getMediaOption(
                isModerator,
                pin,
                captcha,
                captchaId,
                moveMeeting
            )
        )

    }


    override fun onConnected(call: Call?) {

        Thread.setDefaultUncaughtExceptionHandler(GlobalExceptionHandler())
        onCallConnected(
            call?.getCallId().orEmpty(),
            call?.isCUCMCall() ?: false,
            call?.isWebexCallingOrWebexForBroadworks() ?: false
        )

        webexViewModel.setShareMaxCaptureFPSSetting(30)

    }

    override fun onStartRinging(call: Call?, ringerType: Call.RingerType) {
    }

    override fun onStopRinging(call: Call?, ringerType: Call.RingerType) {
    }

    override fun onWaiting(call: Call?) {
    }

    override fun onDisconnected(
        call: Call?,
        event: CallObserver.CallDisconnectedEvent?
    ) {
        Thread.setDefaultUncaughtExceptionHandler(null)
        callManagementServiceIntent?.let {
            activity?.stopService(it)
            callManagementServiceIntent = null
        }
        var callFailed = false
        var callEnded = false

        var failedError: WebexError<Any>? = null
        event?.let { _event ->
            when (_event) {
                is CallObserver.OtherConnected -> {
                    callEnded = true
                }

                is CallObserver.CallErrorEvent -> {
                    failedError = _event.getError()
                    callFailed = true
                }

                is CallObserver.CallEnded -> {
                    callEnded = true
                }

            }
        }

        when {
            callFailed -> {
                onCallFailed(call?.getCallId().orEmpty(), failedError)
            }

            callEnded -> {
                onCallTerminated(call?.getCallId().orEmpty())
            }

        }

    }

    override fun onInfoChanged(call: Call?) {
    }

    override fun onMediaChanged(
        call: Call?,
        event: CallObserver.MediaChangedEvent?
    ) {
        event?.let { _event ->
            val call = _event.getCall()
            when (_event) {
                is CallObserver.RemoteSendingVideoEvent -> {

                    webexViewModel.isRemoteVideoMuted = !_event.isSending()
                    onVideoStreamingChanged(call?.getCallId().orEmpty())
                }

                is CallObserver.SendingVideo -> {

                    webexViewModel.isLocalVideoMuted = !_event.isSending()
                    onVideoStreamingChanged(call?.getCallId().orEmpty())
                }

                is CallObserver.ReceivingVideo -> {
                    webexViewModel.isRemoteVideoMuted = !_event.isReceiving()
                    onVideoStreamingChanged(call?.getCallId().orEmpty())
                }

                is CallObserver.MediaStreamAvailabilityEvent -> {
                    onMediaStreamAvailabilityEvent(
                        call?.getCallId().orEmpty(),
                        _event
                    )
                }
            }
        }
    }

    private fun onMediaStreamAvailabilityEvent(
        callId: String,
        event: CallObserver.MediaStreamAvailabilityEvent
    ) {
        if ((webexViewModel.currentCallId != callId)) {
            return
        }

        mHandler.post {
            if (event.isAvailable()) {
                if (event.getStream()
                        ?.getStreamType() == MediaStreamType.Stream1
                ) {
                    //remote media stream
                    onVideoStreamingChanged(webexViewModel.currentCallId.toString())
                    setRemoteVideoInformation(
                        event.getStream()?.getPerson()?.getDisplayName()
                            .orEmpty(),
                        !(event.getStream()?.getPerson()?.isSendingAudio()
                            ?: true)
                    )
                }
            }

            event.getStream()?.setOnMediaStreamInfoChanged { type, info ->
                mediaStreamInfoChangedListener(type, info)
            }
        }
    }


    override fun onMediaQualityInfoChanged(mediaQualityInfo: Call.MediaQualityInfo) {

        updateNetworkStatusChange(mediaQualityInfo)
    }

    override fun onBroadcastMessageReceivedFromHost(message: String) {
        lifecycleScope.launch(Dispatchers.Main) {
            showDialogWithMessage(
                requireContext(),
                R.string.message_from_host,
                message
            )
        }
    }

    override fun onHostAskingReturnToMainSession() {
        lifecycleScope.launch(Dispatchers.Main) {
            Toast.makeText(
                requireContext(),
                getString(R.string.host_asking_return_to_main),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onJoinableSessionUpdated(breakoutSessions: List<BreakoutSession>) {

    }

    override fun onJoinedSessionUpdated(breakoutSession: BreakoutSession) {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.tvName.text = breakoutSession.getName()
        }
    }

    override fun onReturnedToMainSession() {
        val callInfo =
            webexViewModel.currentCallId?.let { webexViewModel.getCall(it) }
        lifecycleScope.launch(Dispatchers.Main) {
            binding.callingHeader.text = getString(R.string.onCall)
            binding.tvName.text = callInfo?.getTitle()
        }
    }

    override fun onSessionClosing() {
    }

    override fun onSessionEnabled() {
    }

    override fun onSessionJoined(breakoutSession: BreakoutSession) {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.callingHeader.text = getString(R.string.breakout_session)
            binding.tvName.text = breakoutSession.getName()
        }
    }

    override fun onSessionStarted(breakout: Breakout) {

    }

    override fun onBreakoutUpdated(breakout: Breakout) {

    }

    override fun onBreakoutError(error: BreakoutSession.BreakoutSessionError) {
        val errorText =
            "${getString(R.string.breakout_error_occured)} : ${error.name}"
        lifecycleScope.launch(Dispatchers.Main) {
            showDialogWithMessage(
                requireContext(),
                R.string.error_occurred,
                errorText
            )
        }
    }

    override fun onReceivingNoiseInfoChanged(info: ReceivingNoiseInfo) {
    }

    override fun onClosedCaptionsArrived(captions: CaptionItem) {
    }

    override fun onClosedCaptionsInfoChanged(closedCaptionsInfo: ClosedCaptionsInfo) {
    }

    override fun onMoveMeetingFailed(call: Call?) {
        showDialogWithMessage(
            requireContext(),
            R.string.move_meeting_failed,
            getString(R.string.move_meeting_failed_message)
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observerCallLiveData() {
        webexViewModel.setCompositeLayoutLiveData.observe(
            viewLifecycleOwner,
            Observer { result ->
                result?.let {
                    if (it.first) {
                        webexViewModel.compositedVideoLayout =
                            webexViewModel.compositedLayoutState
                    }
                }
            })


        webexViewModel.callingLiveData.observe(viewLifecycleOwner, Observer {
            it?.let {
                val event = it.event
                val call = it.call
                val errorMessage = it.errorMessage

                when (event) {
                    WebexRepository.CallEvent.DialCompleted -> {
                        Log.d(
                            tag,
                            "callingLiveData DIAL_COMPLETED callerId: ${call?.getCallId()}"
                        )
                        onCallJoined(call)
                    }

                    WebexRepository.CallEvent.DialFailed, WebexRepository.CallEvent.WrongApiCalled, WebexRepository.CallEvent.CannotStartInstantMeeting -> {
                        val callActivity = activity as CallActivity?
                        callActivity?.alertDialog(
                            true,
                            errorMessage ?: event.name
                        )
                    }

                    else -> {
                    }
                }
            }
        })
        webexViewModel.forceSendingVideoLandscapeLiveData.observe(
            viewLifecycleOwner,
            Observer { result ->
                if (result) {
                    webexViewModel.isSendingVideoForceLandscape =
                        !webexViewModel.isSendingVideoForceLandscape
                }
            })

        webexViewModel.virtualBgError.observe(
            viewLifecycleOwner,
            Observer { error ->

                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT)
                    .show()

            })

        webexViewModel.annotationEvent.observe(viewLifecycleOwner) { event ->
            when (event) {
                is WebexViewModel.AnnotationEvent.PERMISSION_ASK -> toggleAnnotationPermissionDialog(
                    true,
                    event.personId
                )

                is WebexViewModel.AnnotationEvent.PERMISSION_EXPIRED -> toggleAnnotationPermissionDialog(
                    false,
                    event.personId
                )
            }
        }
        webexViewModel.authLiveData.observe(viewLifecycleOwner, Observer {
            if (it != null && it == Constants.Callbacks.RE_LOGIN_REQUIRED) {
                Log.d(tag, "onReAuthRequired Re login is required by user.")
                onSignedOut()
            }
        })
    }

    private fun onSignedOut() {
        val intent = Intent(requireContext(), JWTLoginActivity::class.java)
        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        webexViewModel.currentCallId?.let {
            webexViewModel.setVideoRenderViews(it)
        }
        webexViewModel.cleanup()
        webexViewModel.callObserverInterface = null
        mHandler.removeCallbacksAndMessages(null)
    }

    private fun setUpViews(bundle: Bundle?) {

        videoViewState(true)
        webexViewModel.callObserverInterface = this

        webexViewModel.enableBackgroundStream(webexViewModel.enableBgStreamtoggle)
        webexViewModel.enableAudioBNR(true)
        webexViewModel.setAudioBNRMode(Phone.AudioBRNMode.HP)
        webexViewModel.setDefaultFacingMode(Phone.FacingMode.USER)

        webexViewModel.setVideoMaxTxFPSSetting(30)
        webexViewModel.setVideoEnableCamera2Setting(true)
        webexViewModel.setVideoEnableDecoderMosaicSetting(true)

        webexViewModel.setSharingMaxRxBandwidth(Phone.DefaultBandwidth.MAX_BANDWIDTH_SESSION.getValue())
        webexViewModel.setAudioMaxRxBandwidth(Phone.DefaultBandwidth.MAX_BANDWIDTH_AUDIO.getValue())

        webexViewModel.setVideoStreamMode(webexViewModel.streamMode)
        binding.callingHeader.text = getString(R.string.calling)
        val callerId =
            bundle?.getString(Constants.Intent.OUTGOING_CALL_CALLER_ID)
        binding.tvName.text = callerId

        binding.ivCancelCall.setOnClickListener(this)
        binding.mainContentLayout.setOnClickListener(this)
        binding.ivNetworkSignal.setOnClickListener(this)
        binding.ivNetworkSignal.visibility = View.GONE
    }


    override fun onClick(v: View?) {
        webexViewModel.currentCallId?.let { callId ->
            when (v) {
                binding.ivCancelCall -> {
                    endCall()
                }


                binding.ivNetworkSignal -> {
                    val text = "Network Status : ${currentNetworkStatus.name}"
                    Toast.makeText(requireContext(), text, Toast.LENGTH_SHORT)
                        .show()
                }

                else -> {
                }
            }
        }
    }

    private fun toggleAnnotationPermissionDialog(
        show: Boolean,
        personID: String?
    ) {
        if (show) {
            annotationPermissionDialog = AlertDialog.Builder(context)
                .setTitle("Live Annotation Permission")
                .setMessage("Annotation request received.")
                .setPositiveButton(getString(R.string.accept)) { _, _ ->
                    webexViewModel.handleAnnotationPermission(true, personID!!)
                }
                .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                    webexViewModel.handleAnnotationPermission(false, personID!!)
                }
                .create()

            annotationPermissionDialog.show()
        } else {
            if (annotationPermissionDialog.isShowing) annotationPermissionDialog.dismiss()
        }
    }

    fun onBackPressed() {
        endCall()
    }


    private fun endCall() {
        webexViewModel.currentCallId?.let {
            webexViewModel.hangup(it)
            activity?.finish()
        } ?: run {
            activity?.finish()
        }
    }

    private fun videoViewTextColorState(hidden: Boolean) {
        var hide = hidden
        if (hide && webexViewModel.isRemoteScreenShareON) {
            hide = false
        }

        if (hide) {
            binding.callingHeader.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.black
                )
            )
            binding.tvName.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.black
                )
            )
        } else {
            val status = isMainStageRemoteVideoUnMuted()
            if (status) {
                binding.callingHeader.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.white
                    )
                )
                binding.tvName.setTextColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.white
                    )
                )
            }
        }
    }

    private fun localVideoViewState(toHide: Boolean) {
        if (toHide) {
            binding.localViewLayout.visibility = View.GONE
        } else {
            binding.localViewLayout.visibility = View.VISIBLE
            binding.localView.setZOrderOnTop(true)
        }
    }

    private fun resizeRemoteVideoView() {

        if (webexViewModel.isRemoteScreenShareON) {
            val width =
                resources.getDimension(R.dimen.remote_video_view_width).toInt()
            val height =
                resources.getDimension(R.dimen.remote_video_view_height).toInt()

            val params = ConstraintLayout.LayoutParams(width, height)
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            params.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
            params.marginStart =
                resources.getDimension(R.dimen.remote_video_view_margin_start)
                    .toInt()
            params.bottomMargin =
                resources.getDimension(R.dimen.remote_video_view_margin_Bottom)
                    .toInt()
            binding.remoteViewLayout.layoutParams = params
            binding.remoteViewLayout.background = ContextCompat.getDrawable(
                requireActivity(),
                R.drawable.surfaceview_border
            )
            binding.remoteView.setZOrderOnTop(true)
        } else {
            val params = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
            params.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID
            params.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
            binding.remoteViewLayout.layoutParams = params
            binding.remoteViewLayout.background = ContextCompat.getDrawable(
                requireActivity(),
                R.drawable.surfaceview_transparent_border
            )
            binding.remoteView.setZOrderOnTop(false)
        }
    }

    private fun videoViewState(toHide: Boolean) {
        localVideoViewState(toHide)
        if (toHide) {
            binding.remoteViewLayout.visibility = View.GONE
        } else {
            val status = isMainStageRemoteVideoUnMuted()
            if (status) {
                binding.remoteViewLayout.visibility = View.VISIBLE
            }
        }

        videoViewTextColorState(toHide)
    }


    private fun onCallConnected(
        callId: String,
        isCucmCall: Boolean,
        isWebexCallingOrWebexForBroadworks: Boolean
    ) {

        if (webexViewModel.currentCallId.isNullOrEmpty() && (isCucmCall || isWebexCallingOrWebexForBroadworks)) {
            webexViewModel.currentCallId = callId
        }

        mHandler.post {
            val layout = webexViewModel.getCompositedLayout()
            binding.ivNetworkSignal.visibility = View.VISIBLE
            webexViewModel.setCompositedLayout(layout)

            if (callId == webexViewModel.currentCallId) {
                val callInfo = webexViewModel.getCall(callId)

                var isSelfVideoMuted = true
                callInfo?.let { _callInfo ->
                    isSelfVideoMuted = !_callInfo.isSendingVideo()
                    webexViewModel.isRemoteVideoMuted =
                        !_callInfo.isReceivingVideo()

                }
                binding.videoCallLayout.visibility = View.VISIBLE

                webexViewModel.isLocalVideoMuted = isSelfVideoMuted

                onVideoStreamingChanged(callId)

                if (webexViewModel.isLocalVideoMuted) {
                    localVideoViewState(true)
                } else {
                    localVideoViewState(false)
                }

                if (webexViewModel.isRemoteVideoMuted) {
                    binding.remoteViewLayout.visibility = View.GONE
                } else {
                    val status = isMainStageRemoteVideoUnMuted()
                    if (status) {
                        binding.remoteViewLayout.visibility = View.VISIBLE
                    }
                }

                videoViewTextColorState(webexViewModel.isRemoteVideoMuted)
            }
        }
    }

    private fun isMediaStreamAlreadyPinned(
        personID: String?,
        streamType: MediaStreamType?
    ): Boolean {
        personID?.let { id ->
            webexViewModel.getMediaStreams()?.let { streamList ->
                val stream =
                    streamList.find { stream -> stream.getStreamType() == streamType }
                stream?.let {
                    return it.isPinned()
                }
            }
        }

        return false
    }


    private fun setRemoteVideoInformation(name: String, audioMuted: Boolean) {
        binding.tvRemoteUserName.text = name

        if (audioMuted) {
            binding.ivRemoteAudioState.setImageResource(R.drawable.ic_microphone_muted_bold)
        } else {
            binding.ivRemoteAudioState.setImageResource(R.drawable.ic_microphone_36)
        }
    }

    private fun onVideoStreamingChanged(callId: String) {

        if (webexViewModel.currentCallId == null) {
            return
        }

        mHandler.post {
            if (isAdded) {
                if (webexViewModel.isLocalVideoMuted) {
                    localVideoViewState(true)
                } else {
                    localVideoViewState(false)
                    val pair = webexViewModel.getVideoRenderViews(callId)
                    if (pair.first == null) {
                        webexViewModel.setVideoRenderViews(
                            callId,
                            binding.localView,
                            binding.remoteView
                        )
                    }
                }

                if (webexViewModel.isRemoteVideoMuted) {
                    binding.remoteViewLayout.visibility = View.GONE

                    binding.ivRemoteAudioState.visibility = View.GONE
                    binding.tvRemoteUserName.visibility = View.GONE
                } else {
                    if (webexViewModel.isRemoteScreenShareON) {
                        resizeRemoteVideoView()
                    }
                    val status = isMainStageRemoteVideoUnMuted()

                    if (status) {
                        binding.remoteViewLayout.visibility = View.VISIBLE
                        val pair = webexViewModel.getVideoRenderViews(callId)
                        if (pair.second == null && webexViewModel.callCapability != WebexRepository.CallCap.Audio_Only) {
                            webexViewModel.setVideoRenderViews(
                                callId,
                                binding.localView,
                                binding.remoteView
                            )
                        }

                        if (webexViewModel.streamMode != Phone.VideoStreamMode.COMPOSITED) {

                            binding.ivRemoteAudioState.visibility =
                                View.VISIBLE
                            binding.tvRemoteUserName.visibility =
                                View.VISIBLE
                        } else {
                            binding.ivRemoteAudioState.visibility = View.GONE
                            binding.tvRemoteUserName.visibility = View.GONE
                        }
                    }
                }

                videoViewTextColorState(webexViewModel.isRemoteVideoMuted)

                if (isInPipMode && !webexViewModel.isRemoteVideoMuted) {
                    localVideoViewState(true)
                    binding.ivRemoteAudioState.visibility = View.GONE
                    binding.tvRemoteUserName.visibility = View.GONE
                }
            }
        }
    }

    private fun isMainStageRemoteVideoUnMuted(): Boolean {
        var status = false
        if (!webexViewModel.isRemoteVideoMuted) {

            val streams = webexViewModel.getMediaStreams()

            streams?.let { streamList ->
                val stream =
                    streamList.find { stream -> stream.getStreamType() == MediaStreamType.Stream1 }
                stream?.let { st ->

                    status = st.getPerson()?.isSendingVideo() ?: false
                }
            } ?: run {
                status = false
            }
        }
        return status
    }


    private fun onCallJoined(call: Call?) {

        mHandler.post {
            if (call?.getCallId().orEmpty() == webexViewModel.currentCallId) {
                showCallHeader(call?.getCallId().orEmpty())
                call?.let {
                    val schedules = it.getSchedules()
                    schedules?.let {
                        binding.callingHeader.text = getString(R.string.meeting)
                    }
                }
            }

        }
    }

    private fun showCallHeader(callId: String) {
        mHandler.post {
            binding.callingHeader.text = getString(R.string.onCall)
        }
    }

    private fun onCallFailed(callId: String, failedError: WebexError<Any>?) {

        mHandler.post {
            if (webexViewModel.isAddedCall) {
                resumePrevCallIfAdded(callId)
                updateCallHeader()
            }

            callFailed = !webexViewModel.isAddedCall

            val callActivity = activity as CallActivity?
            callActivity?.alertDialog(
                !webexViewModel.isAddedCall,
                failedError?.errorMessage.orEmpty()
            )
        }
    }


    private fun onCallTerminated(callId: String) {
        webexViewModel.clearCallObservers(callId)
        CallObjectStorage.removeCallObject(callId)

        mHandler.post {
            if (webexViewModel.isAddedCall) {
                resumePrevCallIfAdded(callId)
                updateCallHeader()
            }

            if (!callFailed && !webexViewModel.isAddedCall) {
                activity?.finish()
            }
            webexViewModel.isAddedCall = false
        }
    }

    private fun resumePrevCallIfAdded(callId: String) {
        //resume old call
        if (callId == webexViewModel.currentCallId) {
            webexViewModel.currentCallId = webexViewModel.oldCallId

            webexViewModel.currentCallId?.let { _currentCallId ->
                webexViewModel.holdCall(_currentCallId)
            }
            webexViewModel.oldCallId =
                null //old is  disconnected need to make it null
        }
    }

    private fun updateCallHeader() {
        webexViewModel.currentCallId?.let {
            showCallHeader(it)
        }
    }

    private fun updateNetworkStatusChange(mediaQualityInfo: Call.MediaQualityInfo) {
        when (mediaQualityInfo) {
            Call.MediaQualityInfo.NetworkLost -> {
                binding.ivNetworkSignal.setImageResource(R.drawable.ic_no_network)
                currentNetworkStatus = NetworkStatus.NoNetwork
            }

            Call.MediaQualityInfo.Good -> {
                binding.ivNetworkSignal.setImageResource(R.drawable.ic_good_network)
                currentNetworkStatus = NetworkStatus.Good
            }

            Call.MediaQualityInfo.PoorUplink -> {
                binding.ivNetworkSignal.setImageResource(R.drawable.ic_poor_network)
                currentNetworkStatus = NetworkStatus.PoorUplink
            }

            Call.MediaQualityInfo.PoorDownlink -> {
                binding.ivNetworkSignal.setImageResource(R.drawable.ic_poor_network)
                currentNetworkStatus = NetworkStatus.PoorDownlink
            }

            Call.MediaQualityInfo.HighCpuUsage -> showDialogWithMessage(
                requireContext(),
                R.string.warning,
                getString(R.string.high_cpu_usage)
            )

            Call.MediaQualityInfo.DeviceLimitation -> showDialogWithMessage(
                requireContext(),
                R.string.warning,
                getString(R.string.device_limitation)
            )
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d("CallControlFragment", "newConfig ${newConfig.orientation}")
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.localViewLayout.layoutParams.height =
                requireActivity().resources.getDimension(R.dimen.local_video_view_width)
                    .toInt()
            binding.localViewLayout.layoutParams.width =
                requireActivity().resources.getDimension(R.dimen.local_video_view_height)
                    .toInt()
        } else {
            binding.localViewLayout.layoutParams.height =
                requireActivity().resources.getDimension(R.dimen.local_video_view_height)
                    .toInt()
            binding.localViewLayout.layoutParams.width =
                requireActivity().resources.getDimension(R.dimen.local_video_view_width)
                    .toInt()
        }
        binding.localViewLayout.requestLayout()
    }

    fun pipVisibility(currentView: Int, inPipMode: Boolean) {
        isInPipMode = inPipMode
        if (currentView == View.GONE) {
            binding.videoCallLayout.layoutParams.height = 400
            if (binding.screenShareView.isVisible) {
                binding.remoteViewLayout.visibility = currentView
            }
        } else {
            binding.videoCallLayout.layoutParams.height =
                resources.getDimension(R.dimen.video_view_height).toInt()
            binding.remoteViewLayout.visibility = currentView
        }
        binding.localViewLayout.visibility = currentView
        binding.viewAuxVideosContainer.visibility = currentView
        binding.ivNetworkSignal.visibility = currentView
        binding.tvRemoteUserName.visibility = currentView
        binding.ivRemoteAudioState.visibility = currentView
        binding.callingHeader.visibility = currentView
        binding.tvName.visibility = currentView
        binding.ivCancelCall.visibility = currentView
    }

    fun aspectRatio(): Rational {
        var width = binding.videoCallLayout.width.toInt()
        var height = binding.videoCallLayout.height.toInt()

        return if (width > 0 && height > 0) {
            getCoercedRational(width, height)
        } else if (UIUtils.isPortraitMode(requireContext())) {
            Rational(9, 16)
        } else {
            Rational(16, 9)
        }
    }

    /**
     * Get rational coerce in (0.476, 2.1)
     * */
    fun getCoercedRational(width: Int, height: Int): Rational {
        return when {
            width.toFloat() / height.toFloat() > 2.1f -> Rational(21, 10)
            width.toFloat() / height.toFloat() < 1 / 2.1f -> Rational(10, 21)
            else -> Rational(width, height)
        }
    }

    override fun onPause() {
        Log.d(tag, "BreakoutSession onPause() called")
        super.onPause()
    }

    override fun onStop() {

        mediaPlayer.reset()
        super.onStop()
    }

    // Remote share callback
    override fun onShareStarted() {

    }

    override fun onShareStopped() {

    }

    override fun onFrameSizeChanged(width: Int, height: Int) {
    }

}