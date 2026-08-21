@file:Suppress("DEPRECATION")

package io.legado.app.data.entities

import io.legado.app.data.appDb

/**
 * 未迁移调用方的只读兼容接缝。持久化模型本身不再取得全局数据库；新代码必须使用
 * BookSourceRepository。调用基线由 verifyConfigArchitecture 冻结，并在 Phase 9 删除。
 */
@Deprecated("Use BookSourceRepository.getBookSource instead")
fun BookSourcePart.getBookSource(): BookSource? =
    appDb.bookSourceDao.getBookSource(bookSourceUrl)

@Deprecated("Resolve parts through BookSourceRepository instead")
fun List<BookSourcePart>.toBookSource(): List<BookSource> = mapNotNull { it.getBookSource() }
