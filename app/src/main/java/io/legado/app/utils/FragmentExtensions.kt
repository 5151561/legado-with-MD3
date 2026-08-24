@file:Suppress("unused")

package io.legado.app.utils

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import io.legado.app.R
import io.legado.app.data.entities.Book
import io.legado.app.help.book.isAudio
import io.legado.app.help.book.isImage
import io.legado.app.help.book.isLocal
import io.legado.app.ui.config.readMangaConfig.ReadMangaConfig
import io.legado.app.ui.widget.dialog.TextDialog

inline fun <reified T : DialogFragment> Fragment.showDialogFragment(
    arguments: Bundle.() -> Unit = {}
) {
    val dialog = T::class.java.getDeclaredConstructor().newInstance()
    val bundle = Bundle()
    bundle.apply(arguments)
    dialog.arguments = bundle
    dialog.show(childFragmentManager, T::class.simpleName)
}

fun Fragment.showDialogFragment(dialogFragment: DialogFragment) {
    dialogFragment.show(childFragmentManager, dialogFragment::class.simpleName)
}

fun Fragment.getCompatColor(@ColorRes id: Int): Int = requireContext().getCompatColor(id)

fun Fragment.getCompatDrawable(@DrawableRes id: Int): Drawable? =
    requireContext().getCompatDrawable(id)

fun Fragment.getCompatColorStateList(@ColorRes id: Int): ColorStateList? =
    requireContext().getCompatColorStateList(id)

inline fun <reified T : Activity> Fragment.startActivity(
    configIntent: Intent.() -> Unit = {}
) {
    startActivity(Intent(requireContext(), T::class.java).apply(configIntent))
}

/** 见 `Context.startActivityForBook`：阅读器还没重做，这里只提示。 */
fun Fragment.startActivityForBook(
    @Suppress("UNUSED_PARAMETER") book: Book,
    @Suppress("UNUSED_PARAMETER") configIntent: Intent.() -> Unit = {},
) {
    requireContext().toastOnUi("阅读器还没重做")
}

fun Fragment.showHelp(fileName: String) {
    val mdText = String(requireContext().assets.open("web/help/md/${fileName}.md").readBytes())
    showDialogFragment(TextDialog(getString(R.string.help), mdText, TextDialog.Mode.MD))
}

val Fragment.isCreated
    get() = lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)
