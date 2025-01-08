package marjandn.webex_flutter_plugin

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val webexModule = module(createdAtStart = true) {
    single { WebexRepository(get()) }

    viewModel {
        WebexViewModel(get(), get())
    }
}