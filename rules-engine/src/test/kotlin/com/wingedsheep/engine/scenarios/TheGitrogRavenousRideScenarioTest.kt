package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.SaddledComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.TheGitrogRavenousRide
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for The Gitrog, Ravenous Ride (OTJ mythic Frog Horror Mount), {3}{B}{G}, 6/5.
 *
 * Trample, haste.
 * Whenever The Gitrog deals combat damage to a player, you may sacrifice a creature that saddled it
 * this turn. If you do, draw X cards, then put up to X land cards from your hand onto the
 * battlefield tapped, where X is the sacrificed creature's power.
 * Saddle 1.
 *
 * Exercises the pipeline-sacrifice → `DynamicAmount.sacrificedPower` → draw/put-lands chain, proving
 * the MoveCollection sacrifice snapshot feeds the downstream X reads.
 */
class TheGitrogRavenousRideScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + TheGitrogRavenousRide)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.isSaddled(id: EntityId): Boolean =
        state.getEntity(id)?.has<SaddledComponent>() == true

    fun GameTestDriver.advanceToSacrificeDecision(): SelectCardsDecision {
        var guard = 0
        while (pendingDecision !is SelectCardsDecision && state.step != Step.POSTCOMBAT_MAIN && guard++ < 40) {
            if (pendingDecision != null) autoResolveDecision()
            else if (state.priorityPlayerId != null) bothPass()
        }
        return pendingDecision as? SelectCardsDecision
            ?: error("Expected a SelectCardsDecision for the sacrifice choice (have $pendingDecision, step ${state.step})")
    }

    test("combat damage while saddled: sacrifice the 2-power saddler, draw 2, put up to 2 lands tapped") {
        val driver = createDriver()
        val gitrog = driver.putCreatureOnBattlefield(driver.player1, "The Gitrog, Ravenous Ride")
        val saddler = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears") // power 2 >= saddle 1
        driver.removeSummoningSickness(gitrog)

        // Two lands in hand to play after drawing.
        driver.putCardInHand(driver.player1, "Forest")
        driver.putCardInHand(driver.player1, "Forest")

        // Saddle Gitrog with the bear.
        driver.submitSuccess(SaddleMount(driver.player1, gitrog, listOf(saddler)))
        driver.bothPass()
        driver.isSaddled(gitrog) shouldBe true

        val landsBefore = driver.getLands(driver.player1).size

        // Attack unblocked; Gitrog deals 6 combat damage to player2.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(gitrog), driver.player2)

        // The combat-damage trigger pauses to ask which saddler to sacrifice; choose the bear.
        val sacDecision = driver.advanceToSacrificeDecision()
        sacDecision.options.contains(saddler) shouldBe true
        driver.submitCardSelection(sacDecision.playerId, listOf(saddler))

        // The bear was sacrificed; X = 2 (its power). Next pause is the "put up to X lands" choice.
        driver.findPermanent(driver.player1, "Grizzly Bears") shouldBe null

        // Pick the two Forests from hand to put onto the battlefield tapped.
        val forestsInHand = driver.getHand(driver.player1).filter {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Forest"
        }
        val landDecision = driver.advanceToSacrificeDecision() // next SelectCardsDecision = land choice
        driver.submitCardSelection(landDecision.playerId, forestsInHand.take(2))

        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // The two chosen Forests are now on the battlefield, tapped.
        driver.getLands(driver.player1).size shouldBe landsBefore + 2
        val tappedForests = driver.getLands(driver.player1).count {
            driver.state.getEntity(it)?.get<CardComponent>()?.name == "Forest" && driver.isTapped(it)
        }
        (tappedForests >= 2) shouldBe true
    }

    test("not saddled: combat damage offers no sacrifice and draws nothing") {
        val driver = createDriver()
        val gitrog = driver.putCreatureOnBattlefield(driver.player1, "The Gitrog, Ravenous Ride")
        driver.removeSummoningSickness(gitrog)
        val handBefore = driver.getHandSize(driver.player1)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(driver.player1, listOf(gitrog), driver.player2)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // No saddler → the "up to one" choice is empty, sacrifice is a no-op, no draw.
        driver.getHandSize(driver.player1) shouldBe handBefore
    }
})
