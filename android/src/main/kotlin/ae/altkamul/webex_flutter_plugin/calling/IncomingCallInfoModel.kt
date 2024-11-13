package ae.altkamul.webex_flutter_plugin.calling

import com.ciscowebex.androidsdk.phone.Call

abstract class IncomingCallInfoModel(var call: Call?) {
    var isEnabled: Boolean = true
}