package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.normalize.Normalizer
import com.wingedsheep.assay.syntax.ParseOutcome
import com.wingedsheep.assay.syntax.parseLine
import com.wingedsheep.assay.syntax.printLine
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The discard-subjects band: "that player discards a card", "Target opponent discards two cards.",
 * "Each opponent discards two cards.", and the causative "you may have target opponent discard a
 * card" that used to be the only row printing a targeted opponent's discard.
 *
 * Every row lands on one `Patterns.Hand` recipe, so what these assert is *who* discards — the
 * reading a byte-perfect round trip cannot check, and the one Headhunter and Silent Specter had
 * wrong.
 */
class HandTest : StringSpec({

    fun fragment(line: String): CardFragment =
        Grammar.abilityLine.parseLine(line).shouldBeInstanceOf<ParseOutcome.Accepted<CardFragment>>().value

    fun roundTrips(line: String) {
        Grammar.abilityLine.printLine(fragment(line)) shouldBe line
    }

    val self = Normalizer.SELF
    val triggeringPlayer = EffectTarget.PlayerRef(Player.TriggeringPlayer)

    "that player is the one the trigger named, and declares no target" {
        val text = "Whenever $self deals combat damage to a player, that player discards a card."
        val script = fragment(text).script
        script.triggeredAbilities.single().effect shouldBe Patterns.Hand.discardCards(1, triggeringPlayer)
        script.targetRequirements shouldBe emptyList()
        roundTrips(text)
        roundTrips("Whenever $self deals combat damage to a player, that player discards two cards.")
        roundTrips("Whenever a player casts a spell, that player discards a card.")
    }

    // Bottomless Pit prints "each player's upkeep", the step family's alternate spelling, so its
    // own line is a VARIANT; the canonical "each upkeep" is what round-trips.
    "that player discards at random over the random recipe" {
        val text = "At the beginning of each upkeep, that player discards a card at random."
        fragment(text).script.triggeredAbilities.single().effect shouldBe
            Patterns.Hand.discardRandom(1, triggeringPlayer)
        roundTrips(text)
    }

    "target opponent discards declares an opponent target" {
        val script = fragment("Target opponent discards two cards.").script
        script.spellEffect shouldBe Patterns.Hand.discardCards(2, Targets.bound())
        script.targetRequirements shouldBe listOf(Targets.opponent())
        roundTrips("Target opponent discards two cards.")
        roundTrips("When $self enters, target opponent discards a card.")
    }

    "each opponent discards a counted number over the per-opponent iteration" {
        fragment("Each opponent discards two cards.").script.spellEffect shouldBe
            Patterns.Hand.eachOpponentDiscards(2)
        roundTrips("Each opponent discards two cards.")
        roundTrips("Each opponent discards a card.")
    }

    // After a declared target "that player" is that target (Ozai's Cruelty) or its owner, not the
    // trigger's player, so the run declines rather than read it as either.
    "that player beside a declared target declines" {
        Slots.namesPlayer(CardScript(spellEffect = Patterns.Hand.discardCards(2, triggeringPlayer)), "TriggeringPlayer") shouldBe true
        Slots.namesPlayer(CardScript(spellEffect = Patterns.Hand.discardCards(2, Targets.bound())), "TriggeringPlayer") shouldBe false
        Grammar.abilityLine.parseLine("$self deals 2 damage to target player. That player discards two cards.")
            .shouldBeInstanceOf<ParseOutcome.Declined>()
    }

    // The causative and the plain sentence are one discard, gated and not; each prints as itself
    // rather than the gated one as "you may target opponent discards a card".
    "the causative keeps its own surface and the plain discard keeps its" {
        roundTrips("When $self enters, you may have target opponent discard a card.")
        roundTrips("You may have target opponent discard a card.")
        roundTrips("Target opponent discards a card.")
    }
})
