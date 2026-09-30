package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers as SdkTriggers
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.effects.ExileUntilLeavesEffect
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The exile-until-leaves band: "exile target creature until ~ leaves the battlefield", one printed
 * sentence that is two abilities — the exile, and the leaves trigger returning the linked card.
 */
class ExileUntilLeavesTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    "an enters trigger reads as the exile plus the linked return" {
        val abilities = fragment(
            "When ~ enters, exile target tapped creature an opponent controls until ~ leaves the battlefield."
        ).script.triggeredAbilities

        abilities.size shouldBe 2
        abilities[0].effect.shouldBeInstanceOf<ExileUntilLeavesEffect>()
        abilities[1].trigger shouldBe SdkTriggers.self.leaves().event
        abilities[1].effect shouldBe Effects.ReturnLinkedExileUnderOwnersControl()
    }

    "the singular quantifiers and a trailing sentence round-trip" {
        roundTrips("When ~ enters, exile target creature until ~ leaves the battlefield.")
        roundTrips(
            "When ~ enters, exile up to one target nonland permanent an opponent controls until ~ leaves " +
                "the battlefield. You gain 2 life."
        )
        roundTrips("When ~ enters, exile up to one other target creature you control until ~ leaves the battlefield.")
    }

    "an activated ability carries the return too" {
        val script = fragment(
            "{2}{W}, {T}: Exile target creature until ~ leaves the battlefield."
        ).script

        script.activatedAbilities.single().effect.shouldBeInstanceOf<ExileUntilLeavesEffect>()
        script.triggeredAbilities.single().effect shouldBe Effects.ReturnLinkedExileUnderOwnersControl()
        roundTrips("{2}{W}, {T}: Exile target creature until ~ leaves the battlefield.")
    }

    // Fail-closed: an exile whose card never comes back is not what the sentence says, so the model
    // without its return trigger must not print as the until-leaves line.
    "an exile-until-leaves without the return does not print" {
        val parsed = fragment("When ~ enters, exile target creature until ~ leaves the battlefield.")
        val stripped = parsed.copy(
            script = CardScript(triggeredAbilities = parsed.script.triggeredAbilities.take(1))
        )
        Grammar.abilityLine.printLine(stripped).shouldBeNull()
    }
})
