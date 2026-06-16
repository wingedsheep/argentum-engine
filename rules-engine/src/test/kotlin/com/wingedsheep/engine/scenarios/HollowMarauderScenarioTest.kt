package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.HollowMarauder
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Hollow Marauder — {6}{B} Creature — Specter Rogue, 4/2, Flying
 *
 * This spell costs {1} less to cast for each creature card in your graveyard.
 * When this creature enters, any number of target opponents each discard a card. For each of those
 * opponents who didn't discard a card with mana value 4 or greater, draw a card.
 */
class HollowMarauderScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(HollowMarauder)
        return driver
    }

    fun GameTestDriver.handNames(playerId: EntityId): List<String> =
        getHand(playerId).mapNotNull { getCardName(it) }

    // Resolve the spell + ETB trigger, answering each decision as it arrives: the controller picks
    // the target opponent, and the opponent picks the card to discard. Falls back to bothPass()
    // to keep the stack moving between decision points.
    fun drivePassesAndDecisions(
        driver: GameTestDriver,
        controller: EntityId,
        opponent: EntityId,
        discardCard: EntityId
    ) {
        var guard = 0
        while (guard++ < 30) {
            when (val decision = driver.pendingDecision) {
                is ChooseTargetsDecision ->
                    driver.submitTargetSelection(decision.playerId, listOf(opponent))
                is SelectCardsDecision ->
                    driver.submitCardSelection(decision.playerId, listOf(discardCard))
                null -> {
                    if (driver.getTopOfStack() == null) return
                    driver.bothPass()
                }
                else -> driver.bothPass()
            }
        }
    }

    test("ETB: opponent discards a low-MV card, controller draws") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        // Opponent has one cheap card (Lightning Bolt, MV 1) to discard.
        val oppCard = driver.putCardInHand(opponent, "Lightning Bolt")

        val marauder = driver.putCardInHand(me, "Hollow Marauder")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 6)

        val handBefore = driver.getHandSize(me)

        driver.submit(
            CastSpell(
                playerId = me,
                cardId = marauder,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        // Resolve the creature spell + ETB trigger, driving each decision as it appears:
        //  - controller chooses the target opponent(s)
        //  - that opponent chooses the card to discard
        drivePassesAndDecisions(driver, me, opponent, oppCard)

        // The Bolt (MV 1 < 4) was discarded → I draw a card.
        driver.getGraveyardCardNames(opponent).contains("Lightning Bolt") shouldBe true
        // Hand: spell left hand (-1), drew 1 (+1) → net unchanged.
        driver.getHandSize(me) shouldBe handBefore
    }

    test("ETB: opponent discards a high-MV card, controller draws nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        // Opponent's only card is Gurmag Angler (MV 7 >= 4).
        val bigCard = driver.putCardInHand(opponent, "Gurmag Angler")

        val marauder = driver.putCardInHand(me, "Hollow Marauder")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 6)

        val handBefore = driver.getHandSize(me)

        driver.submit(
            CastSpell(
                playerId = me,
                cardId = marauder,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true

        drivePassesAndDecisions(driver, me, opponent, bigCard)

        driver.getGraveyardCardNames(opponent).contains("Gurmag Angler") shouldBe true
        // High-MV discard → no draw. Hand: spell left hand (-1).
        driver.getHandSize(me) shouldBe handBefore - 1
    }

    test("cost reduction: {1} less per creature card in graveyard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        // Two creature cards in my graveyard → {6}{B} reduces to {4}{B}.
        driver.putCardInGraveyard(me, "Centaur Courser")
        driver.putCardInGraveyard(me, "Savannah Lions")

        val marauder = driver.putCardInHand(me, "Hollow Marauder")
        // Provide only {4}{B} — enough only if the reduction applied.
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveColorlessMana(me, 4)

        val result = driver.submit(
            CastSpell(
                playerId = me,
                cardId = marauder,
                targets = listOf(ChosenTarget.Player(opponent)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
    }
})
