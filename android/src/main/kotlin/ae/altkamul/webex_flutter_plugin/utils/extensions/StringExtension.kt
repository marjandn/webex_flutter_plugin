package ae.altkamul.webex_flutter_plugin.utils.extensions

import android.util.Patterns

fun CharSequence?.isValidEmail() = !isNullOrEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()


/**
 * Converts offset to the equivalent offset into this String encoded as UTF-8.
 */
fun String.utf8Offset(offset: Int): Int {
    return substring(0, offset).toByteArray().size
}