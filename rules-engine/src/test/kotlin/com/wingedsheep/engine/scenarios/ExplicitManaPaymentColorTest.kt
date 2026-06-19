package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownTurnUpComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ons.cards.BirchloreRangers
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Regression tests for explicit mana payment color enforcement.
 *
 * Bug: When using PaymentStrategy.Explicit (player picks which untapped sources to tap),
 * the engine only validated that the chosen sources existed and were untapped — it never
 * checked that the resulting mana could actually pay the colored cost. So you could turn
 * Birchlore Rangers face up (morph {G}) by tapping a Mountain.
 */
class ExplicitManaPaymentColorTest : FunSpec({

    val allCards = TestCards.all + listOf(BirchloreRangers)

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

    test("morph face-up: cannot pay {G} morph cost by tapping a Mountain") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val rangers = driver.putFaceDownCreature(activePlayer, "Birchlore Rangers")
        driver.removeSummoningSickness(rangers)

        val mountain = driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Forest")

        val result = driver.submit(
            TurnFaceUp(
                playerId = activePlayer,
                sourceId = rangers,
                paymentStrategy = PaymentStrategy.Explicit(listOf(mountain))
            )
        )
        result.isSuccess shouldBe false
        // Creature should still be face-down
        driver.state.getEntity(rangers)?.has<FaceDownComponent>() shouldBe true
    }

    test("morph face-up: can pay {G} morph cost by tapping a Forest") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val rangers = driver.putFaceDownCreature(activePlayer, "Birchlore Rangers")
        driver.removeSummoningSickness(rangers)

        driver.putLandOnBattlefield(activePlayer, "Mountain")
        val forest = driver.putLandOnBattlefield(activePlayer, "Forest")

        val result = driver.submit(
            TurnFaceUp(
                playerId = activePlayer,
                sourceId = rangers,
                paymentStrategy = PaymentStrategy.Explicit(listOf(forest))
            )
        )
        result.isSuccess shouldBe true
        driver.state.getEntity(rangers)?.has<FaceDownComponent>() shouldBe false
    }

    test("cast spell: cannot pay {R} cost by tapping a Forest") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val goblin = driver.putCardInHand(activePlayer, "Goblin Guide")
        driver.putLandOnBattlefield(activePlayer, "Mountain")
        val forest = driver.putLandOnBattlefield(activePlayer, "Forest")

        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = goblin,
                paymentStrategy = PaymentStrategy.Explicit(listOf(forest))
            )
        )
        result.isSuccess shouldBe false
    }

    test("cast spell: can pay {R} cost by tapping a Mountain") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val goblin = driver.putCardInHand(activePlayer, "Goblin Guide")
        val mountain = driver.putLandOnBattlefield(activePlayer, "Mountain")
        driver.putLandOnBattlefield(activePlayer, "Forest")

        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = goblin,
                paymentStrategy = PaymentStrategy.Explicit(listOf(mountain))
            )
        )
        result.isSuccess shouldBe true
    }
})
