package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.normalize.Normalizer
import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Gate
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Kicker's condition in a spell: "If **this spell** was kicked, …" where a permanent's trigger says
 * "if **it** was kicked". One model, `WasKicked`, whose subject is spelled by position.
 */
class KickedSpellTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    fun declines(line: String) {
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    val self = Normalizer.SELF

    "a spell's kicked clause is the WasKicked gate" {
        val text = "$self deals 2 damage to any target. If this spell was kicked, draw a card."
        val last = (fragment(text).script.spellEffect as CompositeEffect).effects.last()
        val gate = last.shouldBeInstanceOf<GatedEffect>().gate.shouldBeInstanceOf<Gate.WhenCondition>()
        gate.condition shouldBe Conditions.WasKicked
        roundTrips(text)
    }

    // The two subjects are disjoint by position: a spell never reads the pronoun, a trigger never
    // reads the spell's noun, so neither model has a second printer.
    "each position reads only its own subject" {
        declines("Draw a card. If it was kicked, draw a card.")
        declines("When $self enters, if this spell was kicked, draw a card.")
        roundTrips("Draw a card. If this spell was kicked, draw a card.")
        roundTrips("When $self enters, if it was kicked, draw a card.")
    }

    // CR 702.33g: a target in the kicked part of a spell is chosen only if the spell was kicked.
    // `Effects.If` over an ordinary requirement would choose it on every cast — Probe's divergence.
    "a kicked consequence that declares a target declines" {
        declines("Draw three cards, then discard two cards. If this spell was kicked, target player discards two cards.")
        declines("Draw a card. If this spell was kicked, target player draws a card.")
    }
})
