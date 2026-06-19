package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownTurnUpComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lgn.cards.Dermoplasm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Dermoplasm.
 *
 * Dermoplasm: {2}{U}
 * Creature — Shapeshifter
 * 1/1
 * Flying
 * Morph {2}{U}{U}
 * When this creature is turned face up, you may put a creature card with a morph
 * ability from your hand onto the battlefield face up. If you do, return this
 * creature to its owner's hand.
 */
class DermoplasmTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Dermoplasm))
        return driver
    }

    fun GameTestDriver.handCardNames(playerId: EntityId): List<String> =
        getHand(playerId).mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }

    val allCards = TestCards.all + listOf(Dermoplasm)

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

    test("turning face up with morph creature in hand — put creature, bounce Dermoplasm") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val activePlayer = driver.activePlayer!!

        // Put Dermoplasm face-down on battlefield
        val dermoplasm = driver.putFaceDownCreature(activePlayer, "Dermoplasm")
        driver.removeSummoningSickness(dermoplasm)

        // Put a morph creature in hand
        val morphCreature = driver.putCardInHand(activePlayer, "Morph Test Creature")

        // Give mana to pay morph cost {2}{U}{U}
        driver.giveMana(activePlayer, Color.BLUE, 4)

        // Turn face up
        driver.submit(TurnFaceUp(playerId = activePlayer, sourceId = dermoplasm))

        // Resolve the triggered ability — may pause for card selection
        driver.bothPass()

        if (driver.isPaused) {
            driver.submitCardSelection(activePlayer, listOf(morphCreature))
        }

        // Morph Test Creature should be on the battlefield
        driver.findPermanent(activePlayer, "Morph Test Creature") shouldNotBe null

        // Dermoplasm should be returned to hand (the "if you do" clause)
        driver.findPermanent(activePlayer, "Dermoplasm") shouldBe null
        driver.handCardNames(activePlayer) shouldContain "Dermoplasm"
    }

    test("turning face up with no morph creature in hand — Dermoplasm stays on battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val activePlayer = driver.activePlayer!!

        // Put Dermoplasm face-down on battlefield
        val dermoplasm = driver.putFaceDownCreature(activePlayer, "Dermoplasm")
        driver.removeSummoningSickness(dermoplasm)

        // No morph creatures in hand — just a non-morph card
        driver.putCardInHand(activePlayer, "Wind Drake")

        // Give mana to pay morph cost {2}{U}{U}
        driver.giveMana(activePlayer, Color.BLUE, 4)

        // Turn face up
        driver.submit(TurnFaceUp(playerId = activePlayer, sourceId = dermoplasm))

        // Resolve the triggered ability
        driver.bothPass()

        // No morph creatures to choose — selection auto-resolves with 0 cards
        // Dermoplasm should stay on battlefield (the "if you do" was NOT satisfied)
        driver.findPermanent(activePlayer, "Dermoplasm") shouldNotBe null
        driver.handCardNames(activePlayer) shouldNotContain "Dermoplasm"
    }

    test("turning face up and declining to put a creature — Dermoplasm stays on battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 40),
            startingLife = 20
        )

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val activePlayer = driver.activePlayer!!

        // Put Dermoplasm face-down on battlefield
        val dermoplasm = driver.putFaceDownCreature(activePlayer, "Dermoplasm")
        driver.removeSummoningSickness(dermoplasm)

        // Put a morph creature in hand
        driver.putCardInHand(activePlayer, "Morph Test Creature")

        // Give mana to pay morph cost {2}{U}{U}
        driver.giveMana(activePlayer, Color.BLUE, 4)

        // Turn face up
        driver.submit(TurnFaceUp(playerId = activePlayer, sourceId = dermoplasm))

        // Resolve the triggered ability — may pause for card selection
        driver.bothPass()

        if (driver.isPaused) {
            // Choose 0 (decline to put anything)
            driver.submitCardSelection(activePlayer, emptyList())
        }

        // Dermoplasm should stay on battlefield (chose not to put anything)
        driver.findPermanent(activePlayer, "Dermoplasm") shouldNotBe null
        driver.handCardNames(activePlayer) shouldNotContain "Dermoplasm"
    }
})
