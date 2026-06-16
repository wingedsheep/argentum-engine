package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.StepBetweenWorlds
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Step Between Worlds (OTJ #70) — {3}{U}{U} Sorcery.
 *
 * "Each player may shuffle their hand and graveyard into their library. Each player who does draws
 * seven cards. Exile Step Between Worlds. Plot {4}{U}{U}."
 *
 * Exercises the per-player optional wheel (ForEachPlayer + MayEffect with decisionMaker = the
 * iteration player) and the spell's selfExile() on resolution.
 */
class StepBetweenWorldsScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(StepBetweenWorlds)
        d.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            skipMulligans = true,
            startingPlayer = 0
        )
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** Answer every pending per-player yes/no until none remains, using [choiceByPlayer]. */
    fun GameTestDriver.resolveEachPlayerMay(choiceByPlayer: Map<Any, Boolean>) {
        var safety = 0
        while (safety < 10) {
            val decision = pendingDecision as? YesNoDecision ?: break
            val choice = choiceByPlayer[decision.playerId] ?: false
            submitYesNo(decision.playerId, choice)
            safety++
        }
    }

    test("a player who chooses yes shuffles hand+graveyard and draws seven; spell is exiled") {
        val d = newDriver()
        val p1 = d.player1
        val p2 = d.player2

        // Put the spell in hand and some fodder in hand + graveyard for p1.
        val step = d.putCardInHand(p1, "Step Between Worlds")
        d.putCardInHand(p1, "Lightning Bolt")
        d.putCardInGraveyard(p1, "Counterspell")
        d.putCardInGraveyard(p1, "Doom Blade")
        d.giveMana(p1, Color.BLUE, 2)
        d.giveColorlessMana(p1, 3)

        d.submit(CastSpell(playerId = p1, cardId = step))
        d.bothPass() // resolve -> per-player "may" decisions

        // p1 says yes (shuffle + draw 7), p2 says no (declines).
        d.resolveEachPlayerMay(mapOf(p1 to true, p2 to false))
        d.bothPass()

        d.isPaused shouldBe false

        // p1 shuffled hand + graveyard away and drew exactly seven.
        d.getHand(p1).size shouldBe 7
        d.getGraveyard(p1).isEmpty() shouldBe true

        // The spell exiled itself on resolution (not in graveyard).
        d.getExileCardNames(p1).contains("Step Between Worlds") shouldBe true
        d.getGraveyardCardNames(p1).contains("Step Between Worlds") shouldBe false
    }

    test("a player who declines neither shuffles nor draws") {
        val d = newDriver()
        val p1 = d.player1
        val p2 = d.player2

        val step = d.putCardInHand(p1, "Step Between Worlds")
        d.putCardInGraveyard(p1, "Counterspell")
        val handBefore = d.getHand(p1).size // includes Step Between Worlds
        val gyBefore = d.getGraveyard(p1).size
        d.giveMana(p1, Color.BLUE, 2)
        d.giveColorlessMana(p1, 3)

        d.submit(CastSpell(playerId = p1, cardId = step))
        d.bothPass()

        // Everyone declines.
        d.resolveEachPlayerMay(mapOf(p1 to false, p2 to false))
        d.bothPass()

        d.isPaused shouldBe false
        // -1 for casting Step Between Worlds, no draw, no shuffle of graveyard.
        d.getHand(p1).size shouldBe handBefore - 1
        d.getGraveyard(p1).size shouldBe gyBefore
    }
})
