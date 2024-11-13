package ae.altkamul.webex_flutter_plugin.calling

import ae.altkamul.webex_flutter_plugin.calling.captions.ClosedCaptionsRepository
import ae.altkamul.webex_flutter_plugin.calling.captions.ClosedCaptionsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val callModule = module {
    viewModel {
        CallViewModel(get())
        ClosedCaptionsViewModel(get())
    }

    single { ClosedCaptionsRepository() }

}