package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.StateTriggeredAbility
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.dsl.Conditions as SdkConditions
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * CR 603.8's state triggers — "When you control no Islands, sacrifice ~." — read into
 * `CardScript.stateTriggeredAbilities` over the shared condition and effect vocabularies.
 */
class StateTriggersTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    fun ability(line: String): StateTriggeredAbility =
        fragment(line).script.stateTriggeredAbilities.single()

    // Dandân's golden, but for its sacrifice: the cards spell it `SacrificeTarget(Self)` where the
    // grammar reads the 201-card majority `SacrificeSelfEffect` — see the README's band section.
    "a state trigger is a condition and an effect, in the SDK's own list" {
        val fragment = fragment("When you control no Islands, sacrifice ~.")
        fragment.script.triggeredAbilities.shouldBeEmpty()
        fragment.script.stateTriggeredAbilities.single() shouldBe StateTriggeredAbility(
            id = AbilityId("state trigger"),
            condition = SdkConditions.YouControl(GameObjectFilter.Land.withSubtype("Island"), negate = true),
            effect = SacrificeSelfEffect,
        )
    }

    "there are no X on the battlefield is the controller-blind sibling" {
        ability("When there are no creatures on the battlefield, sacrifice ~.").condition shouldBe
            SdkConditions.AnyPlayerControls(GameObjectFilter.Creature, negate = true)
    }

    "the condition slot is the whole shared vocabulary" {
        ability("When you control seven or more Thrulls, sacrifice ~.").condition shouldBe
            SdkConditions.YouControlAtLeast(7, GameObjectFilter.Permanent.withSubtype("Thrull"))
    }

    // `StateTriggeredAbility` has no requirement field, so a targeted payoff is a value it cannot hold.
    "a payoff that targets declines rather than dropping the target" {
        Grammar.abilityLine.parseLine("When you control no Islands, destroy target creature.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    "only the When spelling is read" {
        Grammar.abilityLine.parseLine("Whenever you control no Islands, sacrifice ~.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    "every state trigger rule can print what it parses" {
        listOf(
            "When you control no Islands, sacrifice ~.",
            "When you control no lands, sacrifice ~.",
            "When you control no Swamps, sacrifice ~.",
            "When there are no creatures on the battlefield, sacrifice ~.",
            "When there are no lands on the battlefield, sacrifice ~.",
        ).forEach { roundTrips(it) }
    }
})
