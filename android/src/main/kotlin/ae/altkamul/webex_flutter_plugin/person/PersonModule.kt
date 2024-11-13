package ae.altkamul.webex_flutter_plugin.person

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val personModule = module {
    viewModel { PersonViewModel(get()) }

    single { PersonRepository(get()) }
}