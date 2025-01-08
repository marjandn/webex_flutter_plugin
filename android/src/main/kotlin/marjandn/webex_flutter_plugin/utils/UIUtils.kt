package marjandn.webex_flutter_plugin.utils

import android.content.Context
import android.content.res.Configuration

object UIUtils {

    fun isPortraitMode(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    }

    fun isInLandscapeMode(context: Context): Boolean {
        return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }
}