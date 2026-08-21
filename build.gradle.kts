@DisableCachingByDefault(because = "架构验证任务没有输出文件")
abstract class VerifyConfigArchitectureTask : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceRoot: DirectoryProperty

    @get:Internal
    abstract val projectRoot: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val moduleSourceFiles: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val moduleBuildFiles: ConfigurableFileCollection

    @get:Input
    abstract val legacyPreferenceCallBaseline: MapProperty<String, Int>

    @get:Input
    abstract val legacyDaoInjectionBaseline: MapProperty<String, Int>

    @get:Input
    abstract val legacyUiDaoAccessBaseline: MapProperty<String, Int>

    @get:Input
    abstract val legacyAppDbReferenceBaseline: Property<Int>

    /**
     * 已经拥有正式 impl 的 feature API 接口：绑定权归 `:feature:<name>:impl`，
     * `:app` 只加载 Koin module 并提供宿主接缝，不得再自行绑定。
     */
    private val formalImplBoundApis = setOf(
        "io.legado.app.feature.bookshelf.api.BookshelfQuery",
        "io.legado.app.feature.bookshelf.api.BookshelfCommands",
        "io.legado.app.feature.bookshelf.api.BookshelfGroupCommands",
        "io.legado.app.feature.bookshelf.api.BookshelfPreferencesGateway",
        "io.legado.app.feature.rss.api.RssQuery",
        "io.legado.app.feature.rss.api.RssCommands",
        "io.legado.app.feature.catalog.api.CatalogQuery",
        "io.legado.app.feature.catalog.api.CatalogCommands",
        "io.legado.app.feature.ai.api.AiOverviewQuery",
        "io.legado.app.feature.ai.api.AiCommands",
    )

    @TaskAction
    fun verify() {
        val sourceRootDir = sourceRoot.get().asFile
        val projectRootDir = projectRoot.get().asFile
        val kotlinFiles = sourceRootDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
        val preferenceBaseline = legacyPreferenceCallBaseline.get()
        val daoInjectionBaseline = legacyDaoInjectionBaseline.get()
        val uiDaoAccessBaseline = legacyUiDaoAccessBaseline.get()
        val violations = mutableListOf<String>()

        moduleSourceFiles.files
            .asSequence()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val relativePath = file.relativeTo(projectRootDir).invariantSeparatorsPath
                val modulePath = relativePath.substringBefore("/src/")
                    .takeIf { it != relativePath }
                    ?.let { ":" + it.replace('/', ':') }
                    ?: return@forEach
                val text = file.readText()
                val imports = Regex("""^import\s+([^\s]+)""", RegexOption.MULTILINE)
                    .findAll(text)
                    .map { it.groupValues[1] }
                    .toList()

                if (modulePath == ":core:designsystem") {
                    imports.filter {
                        it.startsWith("io.legado.app.data.") ||
                            it.startsWith("io.legado.app.domain.") ||
                            it.startsWith("io.legado.app.model.") ||
                            it.startsWith("io.legado.app.feature.") ||
                            it.startsWith("io.legado.app.ui.")
                    }.forEach { forbiddenImport ->
                        violations += "$relativePath: :core:designsystem 禁止导入 $forbiddenImport"
                    }
                }

                if (modulePath.startsWith(":core:")) {
                    imports.filter { it.startsWith("io.legado.app.feature.") }
                        .forEach { forbiddenImport ->
                            violations += "$relativePath: core 模块禁止依赖 feature：$forbiddenImport"
                        }
                }

                if (modulePath.startsWith(":feature:") && modulePath.endsWith(":ui")) {
                    imports.filter {
                        it.startsWith("io.legado.app.data.dao.") ||
                            it.startsWith("io.legado.app.data.entities.") ||
                            ".impl." in it ||
                            it == "io.legado.app.data.appDb"
                    }.forEach { forbiddenImport ->
                        violations += "$relativePath: feature UI 禁止导入 $forbiddenImport"
                    }
                    if (Regex("""\bappDb\s*\.""").containsMatchIn(text)) {
                        violations += "$relativePath: feature UI 禁止直接访问 appDb"
                    }
                }

                if (modulePath.startsWith(":feature:") && modulePath.endsWith(":api")) {
                    imports.filter {
                        it.startsWith("android.") ||
                            it.startsWith("androidx.compose.") ||
                            it.startsWith("androidx.room.") ||
                            it.startsWith("io.legado.app.data.") ||
                            it.startsWith("io.legado.app.domain.") ||
                            it.startsWith("io.legado.app.model.") ||
                            it.startsWith("io.legado.app.ui.")
                    }.forEach { forbiddenImport ->
                        violations += "$relativePath: feature API 禁止泄漏实现类型 $forbiddenImport"
                    }
                }

                if (modulePath == ":app" &&
                    (relativePath.contains("/data/") || relativePath.contains("/domain/"))
                ) {
                    imports.filter { it.startsWith("io.legado.app.ui.main.bookshelf.") }
                        .forEach { forbiddenImport ->
                            violations += "$relativePath: bookshelf 数据/业务实现禁止反向依赖 $forbiddenImport"
                        }
                }

                if ((modulePath == ":app" || modulePath == ":core:database") &&
                    (relativePath.contains("/data/entities/") ||
                        relativePath.contains("/data/dao/"))
                ) {
                    if (relativePath.startsWith("app/src/main/")) {
                        violations += "$relativePath: Room entity/DAO 已迁入 :core:database，禁止回流到 :app"
                    }
                    imports.filter {
                        it == "io.legado.app.data.appDb" ||
                            it == "io.legado.app.model.ReadBook" ||
                            it.startsWith("io.legado.app.help.") ||
                            it.startsWith("io.legado.app.service.") ||
                            it.startsWith("io.legado.app.ui.") ||
                            it.startsWith("io.legado.app.feature.")
                    }.forEach { forbiddenImport ->
                        violations += "$relativePath: entity/DAO 禁止反向依赖 $forbiddenImport"
                    }
                    if (Regex("""\bappDb\s*\.""").containsMatchIn(text)) {
                        violations += "$relativePath: entity/DAO 禁止直接访问 appDb"
                    }
                }

                // Phase 7/8：已建立正式 impl 的 feature，:app 不得再绑定其 API 接口或重建适配器。
                if (modulePath == ":app" && relativePath.startsWith("app/src/main/")) {
                    imports.filter { it in formalImplBoundApis }.forEach { forbiddenImport ->
                        val feature = forbiddenImport.removePrefix("io.legado.app.feature.")
                            .substringBefore('.')
                        violations += "$relativePath: $feature API 只由 " +
                            ":feature:$feature:impl 绑定，:app 禁止导入 $forbiddenImport"
                    }
                }

                val phase3CompatFiles = mapOf(
                    "settings" to "LegacySettingsAdapter.kt",
                    "readaloud" to "LegacyReadAloudAdapter.kt",
                    "reader" to "LegacyReaderAdapter.kt",
                )
                phase3CompatFiles.forEach { (feature, allowedFile) ->
                    val prefix = "app/src/main/java/io/legado/app/feature/$feature/compat/"
                    if (relativePath.startsWith(prefix)) {
                        if (relativePath != prefix + allowedFile) {
                            violations += "$relativePath: $feature 临时兼容接缝只允许 $allowedFile"
                        }
                        imports.filter {
                            it == "io.legado.app.data.appDb" ||
                                it.startsWith("io.legado.app.data.dao.") ||
                                it.startsWith("io.legado.app.help.config.") ||
                                it.startsWith("io.legado.app.service.") ||
                                (it.startsWith("io.legado.app.ui.") && feature != "readaloud")
                        }.forEach { forbiddenImport ->
                            violations += "$relativePath: $feature 临时适配器禁止扩大到 $forbiddenImport"
                        }
                        if (feature == "readaloud") {
                            imports.filter { it.startsWith("io.legado.app.ui.") &&
                                !it.startsWith("io.legado.app.ui.book.readaloud.player.")
                            }.forEach { forbiddenImport ->
                                violations += "$relativePath: readaloud 服务桥接只允许播放器协调器：$forbiddenImport"
                            }
                        }
                    }
                }


                if (relativePath.startsWith("app/src/main/java/io/legado/app/feature/")) {
                    val allowedCompatFiles = buildSet {
                        phase3CompatFiles.forEach { (feature, fileName) ->
                            add("app/src/main/java/io/legado/app/feature/$feature/compat/$fileName")
                        }
                    }
                    if (relativePath !in allowedCompatFiles) {
                        violations += "$relativePath: :app 禁止新增 feature 业务实现或兼容适配器"
                    }
                }
            }

        moduleBuildFiles.files
            .asSequence()
            .filter { it.isFile }
            .forEach { buildFile ->
                val relativePath = buildFile.relativeTo(projectRootDir).invariantSeparatorsPath
                val sourceModule = ":" + relativePath.removeSuffix("/build.gradle.kts")
                    .removeSuffix("/build.gradle")
                    .replace('/', ':')
                findForbiddenModuleDependencies(sourceModule, buildFile.readText())
                    .forEach { violation -> violations += "$relativePath: $violation" }
            }
        val forbiddenConfigImport = Regex(
            """^import io\.legado\.app\.(?:help\.config\.AppConfig|ui\.config\..*Config)$""",
            RegexOption.MULTILINE,
        )
        val preferenceCall = Regex("""\b(?:getPref|putPref)[A-Za-z0-9_]*\s*\(""")
        val daoImport = Regex(
            """^import io\.legado\.app\.data\.dao\.[A-Za-z0-9_*]+$""",
            RegexOption.MULTILINE,
        )
        val appDbDaoAccess = Regex(
            """(?:\bappDb|io\.legado\.app\.data\.appDb)\.[A-Za-z0-9_]*Dao\b"""
        )
        val readBookConfigWrite = Regex(
            """\bReadBookConfig\.[a-z_][A-Za-z0-9_]*(?:\.[a-z_][A-Za-z0-9_]*)?\s*="""
        )
        val readBookConfigMutationCall = Regex(
            """\bReadBookConfig\.durConfig\.set[A-Za-z0-9_]*\s*\("""
        )
        // 上面两条都按 `ReadBookConfig.` 前缀找，成员 import 之后的裸写一个都看不见：
        // `import io.legado.app.help.config.ReadBookConfig.durConfig`（含 as 别名）之后
        // `durConfig = ...` 就是绕过 gateway 的写——不落盘也不 publishState。
        // 不必管通配 import：Kotlin 不允许从 object 按需导入。
        val readBookConfigMemberImport = Regex(
            """^import io\.legado\.app\.help\.config\.ReadBookConfig\.durConfig\b""",
            RegexOption.MULTILINE,
        )
        // 同样的裸写还能从 `with(ReadBookConfig) { durConfig = ... }`／`ReadBookConfig.apply { }`
        // 这类作用域函数里冒出来。与其枚举作用域函数（还会误伤 ChapterProvider 里只读的
        // `with(ReadBookConfig)`），不如直接盯裸赋值本身：带 `.` 前缀的限定写法归上面那条管。
        val readBookConfigBareWrite = Regex("""(?<![.\w])durConfig\s*=(?!=)""")
        // 文件读写层 ReadStyleRepository 同理是 Koin 单例：谁 inject 谁就能直接 save()
        // 覆盖 readConfig.json，磁盘与 ReadStyleConfigStore 的内存状态就此分叉。
        val styleRepositoryOwners = setOf(
            "io/legado/app/data/repository/ReadStyleRepository.kt",
            "io/legado/app/data/repository/ReadStyleConfigStore.kt",
            "io/legado/app/data/repository/ReadBookStyleConfigRepository.kt",
            "io/legado/app/di/appModule.kt",
        )
        // R4.7：Config 的值字段已是 val，字段写入由编译器拦；剩下的唯一写入口是
        // ReadStyleConfigStore 的列表操作。它是 Koin 单例，谁 inject 谁就能绕过 gateway
        // 改配置且不触发 save/publishState——所以限定只有下面这几个文件能提到这个类型。
        val configStoreOwners = setOf(
            "io/legado/app/data/repository/ReadStyleConfigStore.kt",
            "io/legado/app/data/repository/ReadBookStyleConfigRepository.kt",
            "io/legado/app/help/config/ReadBookConfig.kt",
            "io/legado/app/di/appModule.kt",
        )
        val settingsUpdateDeclaration = Regex(
            """\b(?:class|interface|object|typealias)\s+[A-Za-z0-9_]*SettingsUpdate\b"""
        )
        val updateAllDeclaration = Regex("""\bfun\s+(?:<[^>\n]+>\s*)?updateAll\s*\(""")
        val injectedConfigFiles = setOf(
            "io/legado/app/help/config/AppConfig.kt",
            "io/legado/app/help/config/ReadBookConfig.kt",
            "io/legado/app/help/config/ThemePackageManager.kt",
        )

        kotlinFiles.forEach { file ->
            val text = file.readText()
            val relativePath = file.relativeTo(sourceRootDir).invariantSeparatorsPath
            val displayPath = "app/src/main/java/$relativePath"

            if ("prefDelegate" in text || "prefStateDelegate" in text ||
                "Snapshot.withMutableSnapshot" in text
            ) {
                violations += "$displayPath: 禁止 Snapshot 配置桥"
            }
            if ((relativePath.startsWith("io/legado/app/data/") ||
                    relativePath.startsWith("io/legado/app/domain/")) &&
                forbiddenConfigImport.containsMatchIn(text)
            ) {
                violations += "$displayPath: data/domain 禁止导入全局 Config"
            }
            if (("@Composable" in text || "import androidx.compose" in text) &&
                forbiddenConfigImport.containsMatchIn(text)
            ) {
                violations += "$displayPath: Composable 禁止读取兼容 Config"
            }
            if (file.name.endsWith("Config.kt") &&
                ("mutableStateOf(" in text || "Snapshot.withMutableSnapshot" in text ||
                    "import androidx.compose.runtime.State" in text ||
                    "import androidx.compose.runtime.MutableState" in text)
            ) {
                violations += "$displayPath: 配置门面禁止持有 Compose State"
            }
            if (relativePath !=
                "io/legado/app/data/repository/ReadBookStyleConfigRepository.kt" &&
                (readBookConfigWrite.containsMatchIn(text) ||
                    readBookConfigMutationCall.containsMatchIn(text) ||
                    readBookConfigMemberImport.containsMatchIn(text) ||
                    readBookConfigBareWrite.containsMatchIn(text))
            ) {
                violations += "$displayPath: ReadBookConfig 写入必须经过 ReadStyleGateway"
            }
            if (relativePath !in configStoreOwners && "ReadStyleConfigStore" in text) {
                violations += "$displayPath: 排版配置的写入口只对 ReadStyleGateway 的实现开放，" +
                    "不要注入 ReadStyleConfigStore"
            }
            if (relativePath !in styleRepositoryOwners && "ReadStyleRepository" in text) {
                violations += "$displayPath: readConfig.json 的读写只对 ReadStyleConfigStore 与 " +
                    "ReadStyleGateway 的实现开放，不要注入 ReadStyleRepository"
            }
            if (relativePath in injectedConfigFiles && "GlobalContext" in text) {
                violations += "$displayPath: 配置所有者必须显式注入依赖，禁止 GlobalContext"
            }
            if (settingsUpdateDeclaration.containsMatchIn(text)) {
                violations += "$displayPath: 设置网关禁止重新引入 *SettingsUpdate 分发类型"
            }
            if (relativePath.startsWith("io/legado/app/domain/gateway/") &&
                file.name.endsWith("SettingsGateway.kt") &&
                updateAllDeclaration.containsMatchIn(text)
            ) {
                violations += "$displayPath: 设置网关批量修改必须使用单次 update { copy(...) }"
            }

            val preferenceCalls = preferenceCall.findAll(text).count()
            val allowedCalls = preferenceBaseline[relativePath] ?: 0
            if (preferenceCalls > allowedCalls) {
                violations += "$displayPath: 新增了 ${preferenceCalls - allowedCalls} 个旧偏好调用"
            }

            if (relativePath.startsWith("io/legado/app/ui/") &&
                file.name.contains("ViewModel")
            ) {
                val daoDependencies = daoImport.findAll(text).count() +
                    appDbDaoAccess.findAll(text).count()
                val allowedDaoDependencies = daoInjectionBaseline[relativePath] ?: 0
                if (daoDependencies > allowedDaoDependencies) {
                    violations += "$displayPath: ViewModel 新增了 ${daoDependencies - allowedDaoDependencies} 个 DAO 直连"
                } else if (daoDependencies < allowedDaoDependencies) {
                    violations += "$displayPath: 已减少 DAO 直连，请将基线从 $allowedDaoDependencies 下调到 $daoDependencies"
                }
            }

            if (relativePath.startsWith("io/legado/app/ui/") &&
                !file.name.contains("ViewModel")
            ) {
                val daoDependencies = daoImport.findAll(text).count() +
                    appDbDaoAccess.findAll(text).count()
                val allowedDaoDependencies = uiDaoAccessBaseline[relativePath] ?: 0
                if (daoDependencies > allowedDaoDependencies) {
                    violations += "$displayPath: UI 层新增了 ${daoDependencies - allowedDaoDependencies} 个 DAO 直连"
                } else if (daoDependencies < allowedDaoDependencies) {
                    violations += "$displayPath: 已减少 DAO 直连，请将基线从 $allowedDaoDependencies 下调到 $daoDependencies"
                }
            }
        }

        val appDbReferences = kotlinFiles.sumOf { file ->
            Regex("""\bappDb\b""").findAll(file.readText()).count()
        }
        val allowedAppDbReferences = legacyAppDbReferenceBaseline.get()
        if (appDbReferences > allowedAppDbReferences) {
            violations += "app/src/main/java: 新增了 " +
                "${appDbReferences - allowedAppDbReferences} 个全局 appDb 引用"
        } else if (appDbReferences < allowedAppDbReferences) {
            violations += "app/src/main/java: 已减少全局 appDb 引用，请将基线从 " +
                "$allowedAppDbReferences 下调到 $appDbReferences"
        }

        val sourcePaths = kotlinFiles.mapTo(hashSetOf()) {
            it.relativeTo(sourceRootDir).invariantSeparatorsPath
        }
        (daoInjectionBaseline.keys - sourcePaths).forEach { relativePath ->
            violations += "app/src/main/java/$relativePath: 文件已移除，请删除 DAO 直连基线"
        }
        (uiDaoAccessBaseline.keys - sourcePaths).forEach { relativePath ->
            violations += "app/src/main/java/$relativePath: 文件已移除，请删除 UI DAO 直连基线"
        }

        check(violations.isEmpty()) {
            violations.joinToString(prefix = "配置架构护栏失败:\n", separator = "\n")
        }
    }

    companion object {
        fun findForbiddenModuleDependencies(
            sourceModule: String,
            buildScript: String,
        ): List<String> {
            val targets = Regex("""project\(\s*[\"'](:[^\"']+)[\"']\s*\)""")
                .findAll(buildScript)
                .map { it.groupValues[1] }
                .toSet()

            return buildList {
                targets.forEach { target ->
                    when {
                        sourceModule.startsWith(":feature:") && sourceModule.endsWith(":ui") &&
                            (target == ":app" || target == ":core:database" ||
                                (target.startsWith(":feature:") && target.endsWith(":impl"))) -> {
                            add("$sourceModule 禁止依赖 $target")
                        }

                        sourceModule.startsWith(":feature:") && sourceModule.endsWith(":api") &&
                            (target == ":app" || target.startsWith(":core:") ||
                                (target.startsWith(":feature:") &&
                                    !target.endsWith(":api"))) -> {
                            add("$sourceModule 禁止依赖 $target")
                        }

                        sourceModule.startsWith(":feature:") && sourceModule.endsWith(":impl") &&
                            target == ":app" -> {
                            add("$sourceModule 禁止依赖 :app")
                        }

                        sourceModule.startsWith(":core:") &&
                            (target == ":app" || target.startsWith(":feature:")) -> {
                            add("$sourceModule 禁止依赖 $target")
                        }
                    }
                }
            }
        }
    }
}

@DisableCachingByDefault(because = "架构护栏夹具没有输出文件")
abstract class VerifyArchitectureGuardFixtureTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val fixture: RegularFileProperty

    @TaskAction
    fun verifyFixture() {
        val violations = VerifyConfigArchitectureTask.findForbiddenModuleDependencies(
            sourceModule = ":feature:fixture:ui",
            buildScript = fixture.get().asFile.readText(),
        )
        check(violations.toSet() == setOf(
            ":feature:fixture:ui 禁止依赖 :app",
            ":feature:fixture:ui 禁止依赖 :core:database",
            ":feature:fixture:ui 禁止依赖 :feature:bookshelf:impl",
        )) {
            "架构护栏夹具未被正确拒绝：$violations"
        }

        val apiViolations = VerifyConfigArchitectureTask.findForbiddenModuleDependencies(
            sourceModule = ":feature:fixture:api",
            buildScript = fixture.get().asFile.readText(),
        )
        check(apiViolations.toSet() == setOf(
            ":feature:fixture:api 禁止依赖 :app",
            ":feature:fixture:api 禁止依赖 :core:database",
            ":feature:fixture:api 禁止依赖 :feature:bookshelf:impl",
        )) {
            "feature API 架构护栏夹具未被正确拒绝：$apiViolations"
        }

        val coreViolations = VerifyConfigArchitectureTask.findForbiddenModuleDependencies(
            sourceModule = ":core:fixture",
            buildScript = fixture.get().asFile.readText(),
        )
        check(coreViolations.toSet() == setOf(
            ":core:fixture 禁止依赖 :app",
            ":core:fixture 禁止依赖 :feature:bookshelf:impl",
        )) {
            "core 架构护栏夹具未被正确拒绝：$coreViolations"
        }
    }
}

@DisableCachingByDefault(because = "迁移治理任务没有输出文件")
abstract class VerifyFeatureMigrationGovernanceTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val registryFile: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val appBuildFile: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val appMainSourceFiles: ConfigurableFileCollection

    @get:Internal
    abstract val projectRoot: DirectoryProperty

    @TaskAction
    fun verifyGovernance() {
        val root = projectRoot.get().asFile
        val registry = java.util.Properties().apply {
            registryFile.get().asFile.reader(Charsets.UTF_8).use { load(it) }
        }
        val violations = mutableListOf<String>()
        val featureIds = registry.getProperty("features")
            ?.split(',')
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            .orEmpty()

        if (featureIds.isEmpty()) {
            violations += "迁移登记表必须声明非空 features"
        }
        featureIds.groupingBy { it }.eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { violations += "迁移登记表重复声明 feature：$it" }

        val appBuildText = appBuildFile.get().asFile.readText()
        val declaredFlagEntries = Regex(
            """buildConfigField\(\s*"boolean"\s*,\s*"([A-Z0-9_]+)"\s*,\s*providers\.gradleProperty\("([A-Za-z0-9]+)"\)\.getOrElse\("(true|false)"\)\s*,?\s*\)"""
        ).findAll(appBuildText)
            .map { match ->
                match.groupValues[1] to Pair(match.groupValues[2], match.groupValues[3])
            }
            .filter { (constant, _) ->
                constant.startsWith("USE_COMPOSE_") && constant.endsWith("_FEATURE")
            }
            .toList()
        declaredFlagEntries.groupingBy { it.first }.eachCount()
            .filterValues { it > 1 }
            .keys
            .forEach { violations += "app/build.gradle.kts 重复声明迁移开关：$it" }
        val declaredFlags = declaredFlagEntries.toMap()
        val mentionedBuildConfigFlags = Regex(""""(USE_COMPOSE_[A-Z0-9_]+_FEATURE)"""")
            .findAll(appBuildText)
            .map { it.groupValues[1] }
            .toSet()
        (mentionedBuildConfigFlags - declaredFlags.keys).forEach {
            violations += "app/build.gradle.kts 的迁移开关声明无法按治理格式解析：$it"
        }
        val sourceText = appMainSourceFiles.files
            .asSequence()
            .filter { it.isFile && it.extension in setOf("kt", "java") }
            .joinToString(separator = "\n") { it.readText() }
        val activeFlags = mutableMapOf<String, Pair<String, String>>()
        val gradleProperties = mutableSetOf<String>()
        val buildConfigConstants = mutableSetOf<String>()

        fun required(feature: String, field: String): String {
            val key = "$feature.$field"
            return registry.getProperty(key)?.trim().orEmpty().also { value ->
                if (value.isEmpty()) violations += "$key 不能为空"
            }
        }

        fun resolveRelative(feature: String, field: String, value: String): File? {
            if (value.isEmpty()) return null
            val candidate = File(value)
            if (candidate.isAbsolute || value.split('/', '\\').any { it == ".." }) {
                violations += "$feature.$field 必须是仓库内相对路径：$value"
                return null
            }
            return root.resolve(value)
        }

        featureIds.forEach { feature ->
            if (!Regex("[a-z][a-z0-9]*").matches(feature)) {
                violations += "非法 feature id：$feature"
            }
            val gradleProperty = required(feature, "gradleProperty")
            val buildConfig = required(feature, "buildConfig")
            val cardPath = required(feature, "card")
            val legacyUiPath = required(feature, "legacyUiPath")
            val compatAdapterPath = required(feature, "compatAdapterPath")
            val formalImplModule = required(feature, "formalImplModule")
            val implementationStatus = required(feature, "implementationStatus")
            val implementationEvidence = required(feature, "implementationEvidence")
            val adapterRemovalEvidence = required(feature, "adapterRemovalEvidence")
            val implementationBlocker = required(feature, "implementationBlocker")
            val uiStatus = required(feature, "uiStatus")
            val defaultEnabled = required(feature, "defaultEnabled")
            val removalAllowed = required(feature, "legacyRemovalAllowed")
            val releaseGateEvidence = required(feature, "releaseGateEvidence")
            val legacyUiRemovalEvidence = required(feature, "legacyUiRemovalEvidence")
            val uiBlocker = required(feature, "uiBlocker")

            if (!gradleProperties.add(gradleProperty)) {
                violations += "$feature.gradleProperty 与其他 feature 重复：$gradleProperty"
            }
            if (!buildConfigConstants.add(buildConfig)) {
                violations += "$feature.buildConfig 与其他 feature 重复：$buildConfig"
            }
            if (implementationStatus !in setOf("app_adapter", "formal_impl")) {
                violations += "$feature.implementationStatus 非法：$implementationStatus"
            }
            if (uiStatus !in setOf("experiment", "default_observation", "complete")) {
                violations += "$feature.uiStatus 非法：$uiStatus"
            }
            if (defaultEnabled !in setOf("true", "false")) {
                violations += "$feature.defaultEnabled 必须是 true 或 false"
            }
            if (removalAllowed !in setOf("true", "false")) {
                violations += "$feature.legacyRemovalAllowed 必须是 true 或 false"
            }

            val card = resolveRelative(feature, "card", cardPath)
            if (card != null) {
                if (!card.isFile) {
                    violations += "$feature.card 不存在：$cardPath"
                } else if ("删除条件" !in card.readText() && "删除与后续条件" !in card.readText()) {
                    violations += "$feature.card 必须明确记录删除条件：$cardPath"
                }
            }
            val legacyUi = resolveRelative(feature, "legacyUiPath", legacyUiPath)
            val compatAdapter = resolveRelative(feature, "compatAdapterPath", compatAdapterPath)
            val apiModule = root.resolve("feature/$feature/api")
            val uiModule = root.resolve("feature/$feature/ui")

            when (implementationStatus) {
                "app_adapter" -> {
                    if (formalImplModule != "none") {
                        violations += "$feature app_adapter 状态 formalImplModule 必须为 none"
                    }
                    if (implementationEvidence != "none" || adapterRemovalEvidence != "none") {
                        violations += "$feature app_adapter 状态不得伪造实现切换或适配器删除证据"
                    }
                    if (implementationBlocker == "none") {
                        violations += "$feature app_adapter 状态必须记录正式实现 blocker"
                    }
                    if (compatAdapter?.isFile != true) {
                        violations += "$feature app_adapter 状态兼容适配器不存在：$compatAdapterPath"
                    }
                }

                "formal_impl" -> {
                    if (formalImplModule == "none") {
                        violations += "$feature formal_impl 状态必须登记正式 impl 模块"
                    } else {
                        val impl = resolveRelative(feature, "formalImplModule", formalImplModule)
                        if (impl?.isDirectory != true) {
                            violations += "$feature 正式 impl 模块不存在：$formalImplModule"
                        }
                    }
                    val switchEvidence = resolveRelative(
                        feature,
                        "implementationEvidence",
                        implementationEvidence,
                    )
                    if (implementationEvidence == "none" || switchEvidence?.isFile != true) {
                        violations += "$feature formal_impl 状态必须提供实现切换证据"
                    }
                    val adapterEvidence = resolveRelative(
                        feature,
                        "adapterRemovalEvidence",
                        adapterRemovalEvidence,
                    )
                    if (adapterRemovalEvidence == "none" || adapterEvidence?.isFile != true) {
                        violations += "$feature formal_impl 状态必须提供适配器删除证据"
                    }
                    if (implementationBlocker != "none") {
                        violations += "$feature formal_impl 状态 implementationBlocker 必须为 none"
                    }
                    if (compatAdapter?.exists() == true) {
                        violations += "$feature formal_impl 状态仍残留 app 兼容适配器：$compatAdapterPath"
                    }
                }
            }

            if (!apiModule.isDirectory || !uiModule.isDirectory) {
                violations += "$feature 必须同时存在 api/ui 模块"
            }

            when (uiStatus) {
                "experiment" -> {
                    if (defaultEnabled != "false") {
                        violations += "$feature 实验态必须默认关闭"
                    }
                    if (removalAllowed != "false") {
                        violations += "$feature 实验态禁止删除旧路径"
                    }
                    if (legacyUiRemovalEvidence != "none") {
                        violations += "$feature 实验态 legacyUiRemovalEvidence 必须为 none"
                    }
                    if (releaseGateEvidence != "none") {
                        violations += "$feature 实验态 releaseGateEvidence 必须为 none"
                    }
                    if (uiBlocker == "none") {
                        violations += "$feature 实验态必须记录阻塞删除的条件"
                    }
                    if (legacyUi?.isFile != true) {
                        violations += "$feature 实验态旧 UI 登记不存在：$legacyUiPath"
                    }
                    activeFlags[buildConfig] = gradleProperty to defaultEnabled
                }

                "default_observation" -> {
                    if (defaultEnabled != "true") {
                        violations += "$feature 默认观察态必须默认开启"
                    }
                    if (removalAllowed != "false") {
                        violations += "$feature 默认观察态仍禁止删除旧路径"
                    }
                    if (legacyUiRemovalEvidence != "none") {
                        violations += "$feature 默认观察态 legacyUiRemovalEvidence 必须为 none"
                    }
                    val evidence = resolveRelative(
                        feature,
                        "releaseGateEvidence",
                        releaseGateEvidence,
                    )
                    if (releaseGateEvidence == "none" || evidence?.isFile != true) {
                        violations += "$feature 默认观察态必须提供存在的设备/发布签收证据"
                    }
                    if (uiBlocker == "none") {
                        violations += "$feature 默认观察态必须记录完成删除前的剩余门禁"
                    }
                    if (legacyUi?.isFile != true) {
                        violations += "$feature 默认观察态旧 UI 登记不存在：$legacyUiPath"
                    }
                    activeFlags[buildConfig] = gradleProperty to defaultEnabled
                }

                "complete" -> {
                    if (defaultEnabled != "true") {
                        violations += "$feature 完成态必须记录新实现为默认入口"
                    }
                    if (removalAllowed != "true") {
                        violations += "$feature 完成态必须明确允许删除旧路径"
                    }
                    if (uiBlocker != "none") {
                        violations += "$feature 完成态 uiBlocker 必须为 none"
                    }
                    if (legacyUi?.exists() == true) {
                        violations += "$feature 完成态仍残留旧 UI：$legacyUiPath"
                    }
                    if (buildConfig.isNotEmpty() && Regex("\\b${Regex.escape(buildConfig)}\\b")
                            .containsMatchIn(sourceText)
                    ) {
                        violations += "$feature 完成态仍在主源码引用临时开关 $buildConfig"
                    }
                    val evidence = resolveRelative(
                        feature,
                        "legacyUiRemovalEvidence",
                        legacyUiRemovalEvidence,
                    )
                    if (legacyUiRemovalEvidence == "none" || evidence?.isFile != true) {
                        violations += "$feature 完成态必须提供存在的删除签收证据"
                    }
                    val gate = resolveRelative(
                        feature,
                        "releaseGateEvidence",
                        releaseGateEvidence,
                    )
                    if (releaseGateEvidence == "none" || gate?.isFile != true) {
                        violations += "$feature 完成态必须保留设备/发布签收证据"
                    }
                }
            }

            if (uiStatus != "complete" && buildConfig.isNotEmpty() &&
                !Regex("\\b${Regex.escape(buildConfig)}\\b").containsMatchIn(sourceText)
            ) {
                violations += "$feature 主源码没有消费登记的临时开关 $buildConfig"
            }
        }

        if (declaredFlags != activeFlags) {
            val unregistered = declaredFlags.keys - activeFlags.keys
            val missing = activeFlags.keys - declaredFlags.keys
            val mismatched = declaredFlags.keys.intersect(activeFlags.keys)
                .filter { declaredFlags[it] != activeFlags[it] }
            unregistered.forEach { violations += "app/build.gradle.kts 存在未登记迁移开关：$it" }
            missing.forEach { violations += "迁移登记表中的开关未在 app/build.gradle.kts 声明：$it" }
            mismatched.forEach {
                violations += "迁移开关 $it 的 Gradle property 或默认值与登记表不一致"
            }
        }

        check(violations.isEmpty()) {
            violations.joinToString(prefix = "Feature 迁移治理失败:\n", separator = "\n")
        }
    }
}

buildscript {
    extra.apply {
        set("compile_sdk_version", 36)
        set("build_tool_version", "34.0.0")
    }
}

plugins {
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.download) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

val verifyConfigArchitecture = tasks.register<VerifyConfigArchitectureTask>(
    "verifyConfigArchitecture"
) {
    group = "verification"
    description = "禁止配置架构回退、UI 层(ViewModel 及其它)新增 DAO 直连和新增旧偏好调用"
    sourceRoot.set(layout.projectDirectory.dir("app/src/main/java"))
    projectRoot.set(layout.projectDirectory)
    moduleSourceFiles.from(
        fileTree("app") { include("src/**/*.kt") }.files,
        fileTree("core") { include("*/src/**/*.kt") }.files,
        fileTree("feature") { include("**/src/**/*.kt") }.files,
    )
    moduleBuildFiles.from(
        fileTree("core") { include("*/build.gradle", "*/build.gradle.kts") }.files,
        fileTree("feature") { include("**/build.gradle", "**/build.gradle.kts") }.files,
    )
    legacyPreferenceCallBaseline.set(
        mapOf(
            "io/legado/app/App.kt" to 3,
            "io/legado/app/base/BaseActivity.kt" to 2,
            "io/legado/app/base/BaseService.kt" to 1,
            "io/legado/app/data/repository/CoverAlbumRepository.kt" to 4,
            "io/legado/app/data/repository/HighlightRuleRepository.kt" to 9,
            "io/legado/app/data/repository/HomeDashboardRepository.kt" to 3,
            "io/legado/app/data/repository/ReadRecordRepository.kt" to 1,
            "io/legado/app/data/repository/SettingsRepository.kt" to 7,
            "io/legado/app/help/config/LocalConfig.kt" to 3,
            "io/legado/app/help/config/ThemeConfigStore.kt" to 8,
            "io/legado/app/help/storage/Restore.kt" to 2,
            "io/legado/app/receiver/MediaButtonReceiver.kt" to 2,
            "io/legado/app/service/WebService.kt" to 2,
            "io/legado/app/ui/association/ImportReplaceRuleDialog.kt" to 1,
            "io/legado/app/ui/book/explore/ExploreShowViewModel.kt" to 2,
            "io/legado/app/ui/book/read/ReadBookViewModel.kt" to 2,
            "io/legado/app/ui/book/readRecord/ReadRecordViewModel.kt" to 1,
            "io/legado/app/ui/book/search/SearchViewModel.kt" to 3,
            "io/legado/app/ui/config/CheckSourceConfig.kt" to 1,
            "io/legado/app/ui/config/otherConfig/OtherConfigViewModel.kt" to 1,
            "io/legado/app/ui/replace/ReplaceRuleViewModel.kt" to 2,
            "io/legado/app/utils/ContextExtensions.kt" to 12,
            "io/legado/app/web/socket/BookSearchWebSocket.kt" to 2,
        )
    )
    legacyDaoInjectionBaseline.set(
        mapOf(
            // R2.1 已清零：ReadBookViewModel 的书籍/目录读写全部经 BookRepository。
            // 保留 0 值条目让棘轮继续盯着这个文件——新增一处直连就报红。
            "io/legado/app/ui/book/read/ReadBookViewModel.kt" to 0,
            // 护栏缺席期间（MAD-3 未合并窗口）main 新增的直连，随合并冻结，清理归 Track A/F2
            "io/legado/app/ui/book/readaloud/cloudtts/CloudTtsViewModel.kt" to 13,
        )
    )
    // 非 ViewModel 的 UI 层文件直连 DAO 的历史债，只冻结不修复；
    // 清理时逐条下调/删除。护栏会自动要求"减少了就下调基线"，防止回退。
    legacyUiDaoAccessBaseline.set(
        mapOf(
            "io/legado/app/ui/association/AddToBookshelfDialog.kt" to 5,
            "io/legado/app/ui/association/ImportReplaceRuleDialog.kt" to 1,
            "io/legado/app/ui/association/ImportRssSourceDialog.kt" to 1,
            "io/legado/app/ui/book/bookmark/BookmarkDialog.kt" to 2,
            "io/legado/app/ui/book/changesource/ChangeBookSourceDialog.kt" to 1,
            "io/legado/app/ui/book/group/GroupManageDialog.kt" to 2,
            "io/legado/app/ui/book/group/GroupSelectDialog.kt" to 1,
            "io/legado/app/ui/book/read/ReadBookController.kt" to 3,
            "io/legado/app/ui/book/read/page/provider/TextChapterLayout.kt" to 1,
            // 护栏缺席期间 main 新增（整书页码估算），随合并冻结
            "io/legado/app/ui/book/read/pageestimate/ExactChapterPageCountStore.kt" to 3,
            "io/legado/app/ui/book/search/SearchScope.kt" to 4,
            "io/legado/app/ui/config/bookshelfConfig/BookshelfManageScreenConfig.kt" to 1,
            "io/legado/app/ui/main/MainNavGraph.kt" to 2,
            "io/legado/app/ui/rss/article/RssArticlesCompose.kt" to 1,
            "io/legado/app/ui/rss/read/RssJsExtensions.kt" to 8,
            "io/legado/app/ui/widget/dialog/BottomWebViewDialog.kt" to 1,
            "io/legado/app/ui/widget/keyboard/KeyboardAssistsConfig.kt" to 7,
            "io/legado/app/ui/widget/keyboard/KeyboardToolPop.kt" to 1,
        )
    )
    legacyAppDbReferenceBaseline.set(430)
}

val verifyArchitectureGuardFixture = tasks.register<VerifyArchitectureGuardFixtureTask>(
    "verifyArchitectureGuardFixture"
) {
    group = "verification"
    description = "用非法 feature UI 依赖夹具验证模块依赖护栏"
    fixture.set(layout.projectDirectory.file("tools/architecture-fixtures/feature-ui.gradle.kts"))
}

val verifyFeatureMigrationGovernance = tasks.register<VerifyFeatureMigrationGovernanceTask>(
    "verifyFeatureMigrationGovernance"
) {
    group = "verification"
    description = "校验 Compose feature 灰度开关、迁移卡与旧路径删除许可保持一致"
    registryFile.set(layout.projectDirectory.file("config/compose-feature-migrations.properties"))
    appBuildFile.set(layout.projectDirectory.file("app/build.gradle.kts"))
    appMainSourceFiles.from(
        fileTree("app/src/main") { include("**/*.kt", "**/*.java") }.files,
    )
    projectRoot.set(layout.projectDirectory)
}

verifyConfigArchitecture.configure {
    dependsOn(verifyArchitectureGuardFixture, verifyFeatureMigrationGovernance)
}

subprojects {
    tasks.configureEach {
        if (name.startsWith("assemble") || name.startsWith("compile")) {
            dependsOn(verifyConfigArchitecture)
        }
    }
}
