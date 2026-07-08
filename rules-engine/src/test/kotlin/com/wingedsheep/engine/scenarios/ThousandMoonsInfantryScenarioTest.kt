package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.ThousandMoonsInfantry
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Thousand Moons Infantry — {2}{W} Creature — Human Soldier, 2/4.
 *   "Untap this creature during each other player's untap step."
 *
 * Covers the self-scoped [UntapSelfDuringOtherUntapSteps] static: the creature untaps on a
 * *non-controller's* untap step, while a plain creature the same player controls stays tapped
 * through it.
 */
class ThousandMoonsInfantryScenarioTest : FunSpec({

    // A vanilla creature (no self-untap static) — the negative control for the untap test.
    val PlainSoldier = card("Thousand Moons Test Plain Soldier") {
        manaCost = "{2}{W}"
        typeLine = "Creature — Human Soldier"
        power = 2
        toughness = 2
    }

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(ThousandMoonsInfantry, PlainSoldier))
        initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
    }

    test("untaps during another player's untap step, while a plain creature stays tapped") {
        val d = driver()
        val you = d.activePlayer!!
        val opponent = d.getOpponent(you)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val infantry = d.putPermanentOnBattlefield(you, "Thousand Moons Infantry")
        val plainSoldier = d.putPermanentOnBattlefield(you, "Thousand Moons Test Plain Soldier")

        // Tap both on your own turn.
        d.tapPermanent(infantry)
        d.tapPermanent(plainSoldier)
        d.isTapped(infantry) shouldBe true
        d.isTapped(plainSoldier) shouldBe true

        // Advance into the opponent's turn — their untap step runs along the way.
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe opponent

        // The infantry untapped during the opponent's untap step; the plain soldier did not.
        d.isTapped(infantry) shouldBe false
        d.isTapped(plainSoldier) shouldBe true
    }

    test("stays untapped through its own controller's untap step (no double-untap issue)") {
        val d = driver()
        val you = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val infantry = d.putPermanentOnBattlefield(you, "Thousand Moons Infantry")
        d.tapPermanent(infantry)
        d.isTapped(infantry) shouldBe true

        // Advance into the opponent's turn — their untap step untaps the infantry via the static.
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe d.getOpponent(you)
        // Loop through the rest of the opponent's turn back to your own turn: the opponent's
        // precombat main first, then your next upkeep (past your own untap step, where the
        // infantry untaps as normal — no double-untap issue).
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe you
        d.isTapped(infantry) shouldBe false
    }
})
