import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform") version "2.4.0"
    kotlin("plugin.serialization") version "2.4.0"
}

// Standalone build: no JVM engine, web server, card catalogue, or corpus.
val upstream = projectDir.resolve("../..")
val generated = layout.buildDirectory.dir("generated/checker")
val portableSources = tasks.register("portableSources") {
    val sdk = fileTree(upstream.resolve("mtg-sdk/src/main/kotlin")) { include("**/*.kt") }
    val assay = fileTree(upstream.resolve("oracle-assay/src/main/kotlin")) {
        include("**/syntax/**", "**/grammar/**", "**/normalize/**", "**/compile/**")
        include("**/gate/Touchstone.kt", "**/corpus/OracleCard.kt", "**/corpus/ScryfallJson.kt")
    }
    val tooling = fileTree(upstream.resolve("mtg-sdk-tooling/src/main/kotlin")) {
        include("**/CardValidator.kt", "**/CardLinter.kt")
    }
    inputs.files(sdk, assay, tooling)
    outputs.dir(generated)
    doLast {
        val out = generated.get().asFile
        out.deleteRecursively()
        (sdk.files + assay.files + tooling.files).forEach { source ->
            // Adapt platform plumbing only; share grammar, normalization and validation verbatim.
            var text = source.readText().replace("@JvmInline", "")
            text = text.replace("import java.util.concurrent.atomic.AtomicInteger", "import com.wingedsheep.assay.browser.AtomicInteger")
            text = text.replace("import java.util.UUID", "import com.wingedsheep.assay.browser.UUID")
            if (text.contains("ThreadLocal")) {
                text = text.replaceFirst("\n", "\nimport com.wingedsheep.assay.browser.ThreadLocal\n")
            }
            require(!Regex("import java\\.|import javax\\.").containsMatchIn(text)) {
                "Unexpected platform dependency in ${source.name}; keep the browser boundary explicit."
            }
            val relative = source.invariantSeparatorsPath.substringAfter("/src/main/kotlin/")
            out.resolve(relative).apply { parentFile.mkdirs(); writeText(text) }
        }
    }
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser(); binaries.executable() }
    sourceSets {
        wasmJsMain {
            kotlin.srcDir(generated)
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
            }
        }
    }
}
tasks.matching { it.name == "compileKotlinWasmJs" }.configureEach { dependsOn(portableSources) }
