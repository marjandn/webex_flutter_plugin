package marjandn.webex_flutter_plugin.auth

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.dsl.module

val loginModule = module {

    viewModel { LoginViewModel(get(), get()) }
    single { LoginRepository() }
}