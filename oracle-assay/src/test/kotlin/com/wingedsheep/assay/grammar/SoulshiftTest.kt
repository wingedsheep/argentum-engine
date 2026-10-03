package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulshift
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The soulshift band: "Soulshift 4" is the printed keyword *and* the dies trigger CR 702.46a
 * abbreviates, and the line rule must land both — the way `CardBuilder.soulshift` does.
 */
class SoulshiftTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    "soulshift round-trips" {
        for (line in listOf("Soulshift 1", "Soulshift 4", "Soulshift 7")) {
            Grammar.abilityLine.printLine(fragment(line)) shouldBe line
        }
    }

    "soulshift lands the keyword and the trigger the DSL lowers it to" {
        val read = fragment("Soulshift 4")
        val lowered = card("Lowered") { soulshift(4) }

        read.keywordAbilities shouldBe listOf(KeywordAbility.Numeric(Keyword.SOULSHIFT, 4))
        read.script.triggeredAbilities.single().copy(id = lowered.script.triggeredAbilities.single().id) shouldBe
            lowered.script.triggeredAbilities.single()
    }

    "a soulshift keyword without its trigger does not print" {
        val bare = CardFragment.of(listOf(KeywordAbility.Numeric(Keyword.SOULSHIFT, 4)))
        Grammar.abilityLine.printLine(bare) shouldBe null
    }

    "a soulshift trigger with the wrong number does not print" {
        val read = fragment("Soulshift 4")
        val mismatched = read.copy(keywordAbilities = listOf(KeywordAbility.Numeric(Keyword.SOULSHIFT, 5)))
        Grammar.abilityLine.printLine(mismatched) shouldBe null
        Grammar.abilityLine.printLine(read.copy(script = CardScript.EMPTY)) shouldBe null
    }
})
