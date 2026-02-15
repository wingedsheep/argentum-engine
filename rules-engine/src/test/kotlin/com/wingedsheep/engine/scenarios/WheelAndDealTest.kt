package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.CompositeEffect
import com.wingedsheep.sdk.scripting.DiscardHandEffect
import com.wingedsheep.sdk.scripting.DrawCardsEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.TargetOpponent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Wheel and Deal.
 *
 * Wheel and Deal: {3}{U}
 * Instant
 * Any number of target opponents each discard their hands, then draw seven cards. Draw a card.
 */
class WheelAndDealTest : FunSpec({

    val WheelAndDeal = CardDefinition.instant(
        name = "Wheel and Deal",
        manaCost = ManaCost.parse("{3}{U}"),
        oracleText = "Any number of target opponents each discard their hands, then draw seven cards. Draw a card.",
        script = CardScript.spell(
            targets = arrayOf(TargetOpponent()),
            effect = CompositeEffect(
                listOf(
                    DiscardHandEffect(target = EffectTarget.ContextTarget(0)),
                    DrawCardsEffect(7, target = EffectTarget.ContextTarget(0)),
                    DrawCardsEffect(1, target = EffectTarget.Controller)
                )
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(WheelAndDeal))
        return driver
    }

    test("Wheel and Deal discards opponent's hand and opponent draws 7, controller draws 1") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 30, "Forest" to 30),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val opponentHandBefore = driver.getHandSize(opponent)
        val activeHandBefore = driver.getHandSize(activePlayer)

        // Cast Wheel and Deal targeting opponent
        val wheelAndDeal = driver.putCardInHand(activePlayer, "Wheel and Deal")
        driver.giveMana(activePlayer, Color.BLUE, 4)

        val castResult = driver.castSpell(activePlayer, wheelAndDeal, targets = listOf(opponent))
        castResult.isSuccess shouldBe true

        // Resolve
        driver.bothPass()

        // Opponent should have discarded their entire hand and drawn 7
        driver.getHandSize(opponent) shouldBe 7

        // Verify discard event - opponent's entire previous hand was discarded
        val discardEvents = driver.events.filterIsInstance<CardsDiscardedEvent>()
        discardEvents.any { it.playerId == opponent && it.cardIds.size == opponentHandBefore } shouldBe true

        // Verify opponent draw event (7 cards)
        val drawEvents = driver.events.filterIsInstance<CardsDrawnEvent>()
        drawEvents.any { it.playerId == opponent && it.count == 7 } shouldBe true

        // Verify controller draw event (1 card)
        drawEvents.any { it.playerId == activePlayer && it.count == 1 } shouldBe true

        // Controller hand: started with activeHandBefore, added Wheel and Deal (+1), cast it (-1), drew 1 (+1)
        driver.getHandSize(activePlayer) shouldBe activeHandBefore + 1
    }

})
