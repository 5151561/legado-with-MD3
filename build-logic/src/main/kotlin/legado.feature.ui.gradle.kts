plugins {
    id("legado.android.compose")
    // 每个 feature 在自己的 ui 模块里声明 @Serializable 路由（见各模块的 *Navigation.kt），
    // 没有这个插件时路由能编译却在运行期报「Serializer not found」。
    id("org.jetbrains.kotlin.plugin.serialization")
}
