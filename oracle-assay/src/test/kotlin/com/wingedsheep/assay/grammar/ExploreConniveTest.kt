package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.normalize.Normalizer
import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The explore / connive band (CR 701.44, CR 701.50): "When ~ enters, it explores.", "Whenever ~
 * attacks, it connives.", "Target creature you control explores." and the filtered-trigger "it".
 *
 * Both effects name only their actor, so what these assert is *which* permanent explores or
 * connives in each position — the reading a byte-perfect round trip cannot check.
 */
class ExploreConniveTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    val self = Normalizer.SELF

    "the source explores and connives, whichever way it is named" {
        fragment("When $self enters, it explores.").script.triggeredAbilities.single().effect shouldBe
            Effects.Explore(EffectTarget.Self)
        fragment("Whenever $self attacks, it connives.").script.triggeredAbilities.single().effect shouldBe
            Effects.Connive(EffectTarget.Self)
        roundTrips("When $self enters, $self explores.")
        roundTrips("Whenever $self attacks, $self connives.")
        roundTrips("{3}: $self connives.")
    }

    // In a filtered trigger "it" is the creature that entered — Path of Discovery explores *that*.
    "a filtered trigger's it is the triggering permanent" {
        val text = "Whenever a creature you control enters, it explores."
        fragment(text).script.triggeredAbilities.single().effect shouldBe
            Effects.Explore(EffectTarget.TriggeringEntity)
        roundTrips(text)
    }

    "a target explores or connives as the sentence's subject" {
        val explores = fragment("Target creature you control explores.").script
        explores.spellEffect shouldBe Effects.Explore(Targets.bound())
        explores.targetRequirements.single().optional shouldBe false
        roundTrips("Target creature you control explores.")
        roundTrips("When $self enters, target creature you control connives.")
        roundTrips("When $self enters, up to one target creature you control explores.")
    }

    // `ConniveEffectExecutor` still loots when its subject does not resolve, so a connive over an
    // optional target chosen empty would draw and discard anyway — a different card from the text.
    "an optional connive target declines" {
        Grammar.abilityLine.parseLine("When $self enters, up to one target creature you control connives.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    "a later clause's it connives as the target the first clause chose" {
        roundTrips("Target creature you control gains menace until end of turn. It connives.")
    }
})
