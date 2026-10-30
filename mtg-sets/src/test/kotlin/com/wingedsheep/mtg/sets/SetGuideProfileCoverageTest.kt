package com.wingedsheep.mtg.sets

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan

/**
 * The web client's Set guide (`/sets`) describes every fully playable set — what it is about, how it
 * plays, and its power / speed / complexity ratings — from a hand-written table in
 * `web-client/src/components/setGuide/setProfiles.ts`. A set that becomes complete without an entry
 * there still shows in the guide, but as a bare tile with nothing to choose it by.
 *
 * "Fully playable" is the booster generator's `fullyImplemented`: sealed-supported and not
 * `incomplete`. This fails the PR that finishes a set until its profile is written, the same way
 * the "What's new" announcement is part of finishing one.
 */
class SetGuideProfileCoverageTest : FunSpec({

    val source = SetSourceRoots.repoRoot
        .resolve("web-client/src/components/setGuide/setProfiles.ts")
        .toFile()
        .readText()
    val profiled = Regex("""^ {2}([A-Z0-9]{2,5}): \{""", RegexOption.MULTILINE)
        .findAll(source.substringAfter("export const SET_PROFILES"))
        .map { it.groupValues[1] }
        .toSet()

    test("parses a plausible profile table out of setProfiles.ts") {
        profiled.size shouldBeGreaterThan 40
    }

    test("every fully playable set has a Set guide profile") {
        MtgSetCatalog.all
            .filter { it.sealedSupported && !it.incomplete }
            .map { "${it.code} (${it.displayName})" }
            .filterNot { it.substringBefore(' ') in profiled }
            .shouldBeEmpty()
    }

    test("every profile belongs to a known set") {
        val codes = MtgSetCatalog.all.map { it.code }.toSet()
        profiled.filterNot { it in codes }.shouldBeEmpty()
    }
})
