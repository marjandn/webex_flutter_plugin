package marjandn.webex_flutter_plugin.utils

import android.content.Context
import android.content.DialogInterface
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import marjandn.webex_flutter_plugin.R


fun showDialogWithMessage(context: Context, titleResourceId: Int?, message: String, positiveButtonText: Int = android.R.string.ok) {
    val builder: AlertDialog.Builder = AlertDialog.Builder(context)

    builder.setTitle(titleResourceId ?: R.string.message)
    builder.setMessage(message)

    builder.setPositiveButton(positiveButtonText) { dialog, _ ->
        dialog.dismiss()
    }

    builder.show()
}