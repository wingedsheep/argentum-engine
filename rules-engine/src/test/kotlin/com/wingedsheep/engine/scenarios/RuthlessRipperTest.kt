package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownTurnUpComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ktk.cards.RuthlessRipper
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Ruthless Ripper.
 *
 * Ruthless Ripper: {B}
 * Creature — Human Assassin
 * 1/1
 * Deathtouch
 * Morph—Reveal a black card in your hand.
 * When this creature is turned face up, target player loses 2 life.
 */
class RuthlessRipperTest : FunSpec({

    val allCards = TestCards.all + listOf(RuthlessRipper)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    fun GameTestDriver.putFaceDownCreature(playerId: EntityId, cardName: String): EntityId {
        val creatureId = putCreatureOnBattlefield(playerId, cardName)
        val cardDef = allCards.first { it.name == cardName }
        val morphAbility = cardDef.keywordAbilities
            .filterIsInstance<KeywordAbility.Morph>()
            .firstOrNull()
        replaceState(state.updateEntity(creatureId) { container ->
            var c = container.with(FaceDownComponent)
            if (morphAbility != null) {
                c = c.with(FaceDownTurnUpComponent(morphAbility.morphCost, cardDef.name))
            }
            c
        })
        return creatureId
    }

    test("turn face up by revealing a black card from hand") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Swamp" to 40),
            startingLife = 20
        )

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.player2

        // Put Ruthless Ripper face-down on battlefield
        val ripper = driver.putFaceDownCreature(activePlayer, "Ruthless Ripper")
        driver.removeSummoningSickness(ripper)

        // Put a second Ruthless Ripper in hand (it's black, so it can be revealed)
        val blackCardInHand = driver.putCardInHand(activePlayer, "Ruthless Ripper")

        // Turn face up. The reveal morph cost is now paid through CostPaymentService, so the
        // action pauses for a card-selection decision (which black card to reveal) before the flip.
        val result = driver.submit(TurnFaceUp(playerId = activePlayer, sourceId = ripper))
        (result.error == null) shouldBe true
        // Still face-down until the cost is paid.
        driver.state.getEntity(ripper)?.get<FaceDownComponent>() shouldBe FaceDownComponent

        // Pay the cost by revealing the black card.
        driver.submitCardSelection(activePlayer, listOf(blackCardInHand))

        // The card should now be face up
        driver.state.getEntity(ripper)?.get<FaceDownComponent>() shouldBe null
        driver.state.getEntity(ripper)?.get<CardComponent>()?.name shouldBe "Ruthless Ripper"

        // The revealed card should still be in hand (reveal doesn't discard)
        driver.getHand(activePlayer).contains(blackCardInHand) shouldBe true

        // Triggered ability: target player loses 2 life
        // Select target player (opponent)
        driver.submitTargetSelection(activePlayer, listOf(opponent))

        // Resolve the triggered ability
        driver.bothPass()

        // Opponent should have lost 2 life
        driver.assertLifeTotal(opponent, 18)
    }

    test("cannot turn face up without a black card in hand") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val activePlayer = driver.activePlayer!!

        // Put Ruthless Ripper face-down on battlefield
        val ripper = driver.putFaceDownCreature(activePlayer, "Ruthless Ripper")
        driver.removeSummoningSickness(ripper)

        // No black cards in hand — turn face up should fail
        val result = driver.submit(
            TurnFaceUp(
                playerId = activePlayer,
                sourceId = ripper,
                costTargetIds = emptyList()
            )
        )
        result.isSuccess shouldBe false
    }
})
