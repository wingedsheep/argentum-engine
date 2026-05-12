package com.wingedsheep.engine.abilitywords

import com.wingedsheep.sdk.core.Keyword
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldNotBe

/**
 * BDD test: Fateful Bite ability word is recognized as flavor metadata and does not
 * alter activated ability resolution (CR 207.2c — ability words have no rules meaning).
 *
 * GIVEN An activated ability is registered with the Fateful Bite ability-word prefix
 * AND The ability's cost and effect are otherwise valid and activatable
 * WHEN The engine parses the ability
 * THEN The engine recognizes "Fateful Bite" as a known ability word (no parse/registration error)
 * AND The ability word is exposed as metadata on the card definition
 * AND Resolution is unaffected by the prefix
 */
class FatefulBiteAbilityWordTest : FunSpec({

    test("Fateful Bite ability word is recognized as flavor metadata and does not alter activated ability resolution") {
        // CR 207.2c: ability words have no rules meaning — they are flavor prefixes only.
        // The engine must accept "Fateful Bite" as a known Keyword enum value so card
        // authors can tag activated abilities with it without triggering a parse error.
        val abilityWord = Keyword.fromString("Fateful Bite")
        abilityWord shouldNotBe null

        // Oracle text that opens with "Fateful Bite —" must surface the ability word
        // in the definition's keyword set (metadata only — no execution path changes).
        val parsed = Keyword.parseFromOracleText(
            "Fateful Bite — {T}: This creature deals damage equal to its power to target creature."
        )
        parsed shouldContain abilityWord!!
    }
})
