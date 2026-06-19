package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownTurnUpComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ktk.cards.SaguMauler
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Sagu Mauler:
 * {4}{G}{U} Creature — Beast 6/6
 * Trample, hexproof
 * Morph {3}{G}{U}
 */
class SaguMaulerTest : FunSpec({

    val allCards = TestCards.all + listOf(SaguMauler)
    val projector = StateProjector()

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
            .first()
        replaceState(state.updateEntity(creatureId) { container ->
            container.with(FaceDownComponent).with(FaceDownTurnUpComponent(morphAbility.morphCost, cardDef.name))
        })
        return creatureId
    }

    test("Sagu Mauler has trample and hexproof and is 6/6") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mauler = driver.putCreatureOnBattlefield(activePlayer, "Sagu Mauler")
        val projected = projector.project(driver.state)

        projected.hasKeyword(mauler, Keyword.TRAMPLE) shouldBe true
        projected.hasKeyword(mauler, Keyword.HEXPROOF) shouldBe true
        projected.getPower(mauler) shouldBe 6
        projected.getToughness(mauler) shouldBe 6
    }

    test("can be cast face down for 3 generic mana") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val maulerCard = driver.putCardInHand(activePlayer, "Sagu Mauler")
        driver.giveMana(activePlayer, Color.GREEN, 3)

        val result = driver.submit(
            CastSpell(
                playerId = activePlayer,
                cardId = maulerCard,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true
        driver.stackSize shouldBe 1
    }

    test("can be turned face up for morph cost and regains keywords") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mauler = driver.putFaceDownCreature(activePlayer, "Sagu Mauler")
        driver.removeSummoningSickness(mauler)

        // Give mana for morph cost {3}{G}{U}
        driver.giveMana(activePlayer, Color.GREEN, 4)
        driver.giveMana(activePlayer, Color.BLUE, 1)

        val result = driver.submit(
            TurnFaceUp(
                playerId = activePlayer,
                sourceId = mauler,
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        result.isSuccess shouldBe true

        // Should now be face up with trample and hexproof
        driver.state.getEntity(mauler)!!.has<FaceDownComponent>() shouldBe false

        val projected = projector.project(driver.state)
        projected.hasKeyword(mauler, Keyword.TRAMPLE) shouldBe true
        projected.hasKeyword(mauler, Keyword.HEXPROOF) shouldBe true
        projected.getPower(mauler) shouldBe 6
        projected.getToughness(mauler) shouldBe 6
    }
})
