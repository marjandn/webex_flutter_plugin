package ae.altkamul.webex_flutter_plugin

import ae.altkamul.webex_flutter_plugin.utils.PermissionsHelper
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val mainAppModule = module {
    single { PermissionsHelper(androidContext()) }
}