package io.legado.app.data.entities

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import io.legado.app.data.runtime.BookChapterModelRuntimeRegistry
import io.legado.app.data.runtime.EntityVariableRuntime
import io.legado.app.model.analyzeRule.RuleDataInterface
import io.legado.app.utils.GSON
import io.legado.app.utils.fromJsonObject
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.security.MessageDigest

@Parcelize
@Entity(
    tableName = "chapters",
    primaryKeys = ["url", "bookUrl"],
    indices = [(Index(value = ["bookUrl"], unique = false)),
        (Index(value = ["bookUrl", "index"], unique = true))],
    foreignKeys = [(ForeignKey(
        entity = Book::class,
        parentColumns = ["bookUrl"],
        childColumns = ["bookUrl"],
        onDelete = ForeignKey.CASCADE
    ))]
)    // 删除书籍时自动删除章节
data class BookChapter(
    var url: String = "",               // 章节地址
    var title: String = "",             // 章节标题
    var isVolume: Boolean = false,      // 是否是卷名
    @ColumnInfo(defaultValue = "0")
    var tocLevel: Int = 0,              // 目录层级，0 为顶层
    var baseUrl: String = "",           // 用来拼接相对url
    var bookUrl: String = "",           // 书籍地址
    var index: Int = 0,                 // 章节序号
    var isVip: Boolean = false,         // 是否VIP
    var isPay: Boolean = false,         // 是否已购买
    var resourceUrl: String? = null,    // 音频真实URL
    var tag: String? = null,            // 更新时间或其他章节附加信息
    var wordCount: String? = null,      // 本章节字数
    var start: Long? = null,            // 章节起始位置
    var end: Long? = null,              // 章节终止位置
    var startFragmentId: String? = null,  //EPUB书籍当前章节的fragmentId
    var endFragmentId: String? = null,    //EPUB书籍下一章节的fragmentId
    var variable: String? = null,        //变量
    var reviewImg: String? = null        //段评图标
) : Parcelable, RuleDataInterface {

    @delegate:Transient
    @delegate:Ignore
    @IgnoredOnParcel
    override val variableMap: HashMap<String, String> by lazy {
        GSON.fromJsonObject<HashMap<String, String>>(variable).getOrNull() ?: hashMapOf()
    }

    @Ignore
    @IgnoredOnParcel
    var titleMD5: String? = null

    override fun putVariable(key: String, value: String?): Boolean {
        if (super.putVariable(key, value)) {
            variable = GSON.toJson(variableMap)
        }
        return true
    }

    override fun putBigVariable(key: String, value: String?) {
        EntityVariableRuntime.store.putChapter(bookUrl, url, key, value)
    }

    override fun getBigVariable(key: String): String? {
        return EntityVariableRuntime.store.getChapter(bookUrl, url, key)
    }

    override fun hashCode() = url.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other is BookChapter) {
            return other.url == url
        }
        return false
    }

    fun primaryStr(): String {
        return bookUrl + url
    }

    fun getDisplayTitle(
        replaceRules: List<ReplaceRule>? = null,
        useReplace: Boolean = true,
        chineseConvert: Boolean = true,
        chineseConverterType: Int,
    ): String {
        return BookChapterModelRuntimeRegistry.runtime.displayTitle(
            chapter = this,
            replaceRules = replaceRules,
            useReplace = useReplace,
            chineseConvert = chineseConvert,
            chineseConverterType = chineseConverterType,
        )
    }

    fun getAbsoluteURL(): String {
        return BookChapterModelRuntimeRegistry.runtime.absoluteUrl(this)
    }

    private fun ensureTitleMD5Init() {
        if (titleMD5 == null) {
            val digest = MessageDigest.getInstance("MD5")
                .digest(title.toByteArray())
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
            titleMD5 = digest.substring(8, 24)
        }
    }

    @SuppressLint("DefaultLocale")
    @Suppress("unused")
    fun getFileName(suffix: String = "nb"): String {
        ensureTitleMD5Init()
        return String.format("%05d-%s.%s", index, titleMD5, suffix)
    }

    @SuppressLint("DefaultLocale")
    @Suppress("unused")
    fun getFontName(): String {
        ensureTitleMD5Init()
        return String.format("%05d-%s.ttf", index, titleMD5)
    }

}
