package io.legado.app.help.book

import android.content.Context
import io.legado.app.R
import io.legado.app.data.entities.BookGroup
import io.legado.app.data.entities.ReplaceRule
import io.legado.app.data.entities.SearchBook
import io.legado.app.exception.NoStackTraceException

fun BookGroup.getManageName(context: Context): BookGroup.GroupNameInfo = when (groupId) {
    BookGroup.IdAll -> BookGroup.GroupNameInfo(groupName, context.getString(R.string.all))
    BookGroup.IdAudio -> BookGroup.GroupNameInfo(groupName, context.getString(R.string.audio))
    BookGroup.IdLocal -> BookGroup.GroupNameInfo(groupName, context.getString(R.string.local))
    BookGroup.IdNetNone -> BookGroup.GroupNameInfo(groupName, context.getString(R.string.net_no_group))
    BookGroup.IdLocalNone -> BookGroup.GroupNameInfo(groupName, context.getString(R.string.local_no_group))
    BookGroup.IdManga -> BookGroup.GroupNameInfo(groupName, context.getString(R.string.manga))
    BookGroup.IdText -> BookGroup.GroupNameInfo(groupName, context.getString(R.string.noval))
    BookGroup.IdError -> BookGroup.GroupNameInfo(groupName, context.getString(R.string.update_book_fail))
    BookGroup.IdReading -> BookGroup.GroupNameInfo(groupName, context.getString(R.string.is_reading))
    BookGroup.IdUnread -> BookGroup.GroupNameInfo(groupName, context.getString(R.string.is_unread))
    BookGroup.IdReadFinished -> BookGroup.GroupNameInfo(groupName, context.getString(R.string.is_read_finished))
    BookGroup.IdReadFinishedUpdate -> BookGroup.GroupNameInfo(groupName, context.getString(R.string.is_read_finished_update))
    BookGroup.IdReadFinishedComplete -> BookGroup.GroupNameInfo(groupName, context.getString(R.string.is_read_finished_complete))
    else -> BookGroup.GroupNameInfo(groupName)
}

fun SearchBook.trimIntro(context: Context): String {
    val value = intro?.trim()
    return if (value.isNullOrEmpty()) {
        context.getString(R.string.intro_show_null)
    } else {
        context.getString(R.string.intro_show, value)
    }
}

fun ReplaceRule.checkValid() {
    if (!isValid()) throw NoStackTraceException(appInvalidRuleMessage())
}

private fun appInvalidRuleMessage(): String =
    splitties.init.appCtx.getString(R.string.replace_rule_invalid)
