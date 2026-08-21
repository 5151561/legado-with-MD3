package io.legado.app.domain.model

/**
 * `ai_task_presets.taskType` 的取值表。它是持久化列的词表，因此与 schema 同住 `:core:database`，
 * 供 `:app` 与 AI feature 实现共用同一份常量。
 */
object AiTaskType {
    const val CHAT = "chat"
    const val TRANSLATE_CHAPTER = "translate_chapter"
    const val SUMMARIZE_CHAPTER = "summarize_chapter"
    const val SUMMARIZE_BOOK = "summarize_book"
    const val EXPLAIN_SELECTION = "explain_selection"
    const val CLEAN_SELECTION = "clean_selection"
    const val TEXT_FACTORY = "text_factory"
    const val REWRITE_TEXT = "rewrite_text"
    const val ANALYZE_SPEECH = "analyze_speech"
    const val IDENTIFY_CHARACTERS = "identify_characters"
    const val BOOKSHELF_AUTO_GROUP = "bookshelf_auto_group"
}
