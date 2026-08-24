package com.wingedsheep.sdk.core

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import java.io.File

/**
 * `CounterType` is mirrored by hand in `web-client/src/types/enums.ts`, and nothing enforced the two
 * matching. Eleven types had drifted out of the client — storage (City of Shadows), hunger
 * (Fasting), doom, fire, conqueror, net, silver, fate, aim, spore, defense. The failure is silent
 * and invisible: the server sends the counter, the client has no name for it, so it renders nowhere
 * and the only way to notice is to put that one card on a battlefield and look.
 *
 * The mirror also feeds `CounterTypeDisplayNames`, which TypeScript checks for exhaustiveness — so
 * an entry added here without a label fails the client build rather than rendering blank.
 *
 * Lives on the engine side because this is where the source of truth is; the client can't typecheck
 * a filesystem-reading test without `@types/node`.
 */
class CounterTypeClientMirrorTest : DescribeSpec({

    describe("the web client's CounterType mirror") {

        val enumsTs = repoFile("web-client/src/types/enums.ts")
        val source = enumsTs.readText()
        val clientTypes = parseTsEnum(source, "CounterType")

        it("parses a plausible enum out of enums.ts") {
            // Guards the parser: a reshape of enums.ts must fail loudly here, not "pass" by
            // comparing an empty list against the engine's.
            clientTypes.size shouldBeGreaterThan 50
            clientTypes shouldContain "PLUS_ONE_PLUS_ONE"
        }

        it("declares every engine counter type") {
            val engineTypes = CounterType.entries.map { it.name }
            engineTypes.filterNot { it in clientTypes }.shouldBeEmpty()
        }

        it("declares no counter type the engine does not have") {
            val engineTypes = CounterType.entries.map { it.name }.toSet()
            clientTypes.filterNot { it in engineTypes }.shouldBeEmpty()
        }

        it("gives every counter type a display name") {
            val labelled = Regex("""\[CounterType\.([A-Z][A-Z0-9_]*)]\s*:""")
                .findAll(source.substringAfter("CounterTypeDisplayNames"))
                .map { it.groupValues[1] }
                .toSet()
            clientTypes.filterNot { it in labelled }.shouldBeEmpty()
        }

        it("names the counters that were invisible before") {
            clientTypes shouldContain CounterType.STORAGE.name
            clientTypes shouldContain CounterType.HUNGER.name
        }
    }
}) {
    companion object {
        /**
         * Resolve [relative] against the repository root, found by walking up from the working
         * directory (Gradle runs tests from the module dir, not the root).
         */
        private fun repoFile(relative: String): File {
            var dir: File? = File(System.getProperty("user.dir")).absoluteFile
            while (dir != null) {
                val candidate = File(dir, relative)
                if (candidate.isFile) return candidate
                dir = dir.parentFile
            }
            error("Could not find $relative above ${System.getProperty("user.dir")}")
        }

        /** The constant names declared in `export enum <name> { … }`, ignoring comments. */
        private fun parseTsEnum(source: String, name: String): List<String> {
            val body = source.substringAfter("export enum $name {").substringBefore("\n}")
            val withoutComments = body
                .replace(Regex("""/\*[\s\S]*?\*/"""), "")
                .replace(Regex("""//.*$""", RegexOption.MULTILINE), "")
            return Regex("""^\s*([A-Z][A-Z0-9_]*)\s*=""", RegexOption.MULTILINE)
                .findAll(withoutComments)
                .map { it.groupValues[1] }
                .toList()
        }
    }
}
