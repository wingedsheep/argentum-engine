package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.j25.cards.WoodlandLiege
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Woodland Liege (J25 #27) — "Whenever a Beast you control enters, draw a card."
 */
class WoodlandLiegeScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + WoodlandLiege)
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putCreatureOnBattlefield(d.player1, "Woodland Liege")
        return d
    }

    test("a Beast you control entering draws a card") {
        val d = driver()
        val beast = d.putCardInHand(d.player1, "Trample Beast")
        d.giveMana(d.player1, Color.GREEN, 5)
        d.castSpell(d.player1, beast).outcome shouldBe Outcome.Done
        val handAfterCast = d.getHandSize(d.player1)

        d.bothPass() // Beast resolves; Liege triggers
        d.assertStackSize(1, "Woodland Liege triggers")
        d.bothPass()

        d.getHandSize(d.player1) shouldBe handAfterCast + 1
    }

    test("a non-Beast entering doesn't trigger") {
        val d = driver()
        val bears = d.putCardInHand(d.player1, "Grizzly Bears")
        d.giveMana(d.player1, Color.GREEN, 2)
        d.castSpell(d.player1, bears).outcome shouldBe Outcome.Done
        val handAfterCast = d.getHandSize(d.player1)

        d.bothPass()

        withClue("Grizzly Bears is a Bear, not a Beast") {
            d.assertStackSize(0)
            d.getHandSize(d.player1) shouldBe handAfterCast
        }
    }
})
