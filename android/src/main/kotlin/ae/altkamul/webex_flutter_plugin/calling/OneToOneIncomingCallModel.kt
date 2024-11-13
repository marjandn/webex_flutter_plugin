package ae.altkamul.webex_flutter_plugin.calling

import com.ciscowebex.androidsdk.phone.Call

data class OneToOneIncomingCallModel(val _call: Call?): IncomingCallInfoModel(_call)