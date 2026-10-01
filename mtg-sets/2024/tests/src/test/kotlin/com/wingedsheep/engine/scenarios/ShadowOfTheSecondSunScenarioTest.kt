package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.ShadowOfTheSecondSun
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Shadow of the Second Sun (MH3) — at the beginning of each of enchanted player's postcombat main
 * phases, there is an additional beginning phase after this phase: that player untaps, upkeeps and
 * draws again in the same turn, then goes to the end step.
 */
class ShadowOfTheSecondSunScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + ShadowOfTheSecondSun)
        it.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // Past turn 1, whose draw step the starting player skips (CR 103.8a).
        it.replaceState(it.state.copy(turnNumber = 3))
    }

    fun GameTestDriver.enchant(player: EntityId) {
        val shadow = putCardInHand(player1, "Shadow of the Second Sun")
        giveMana(player1, Color.BLUE, 6)
        castSpell(player1, shadow, targets = listOf(player)).error shouldBe null
        bothPass()
        findPermanent(player1, "Shadow of the Second Sun") shouldNotBe null
    }

    test("enchanted player gets an additional untap, upkeep and draw after their postcombat main") {
        val d = driver()
        d.enchant(d.player1)
        val land = d.putLandOnBattlefield(d.player1, "Island")
        d.tapPermanent(land)
        val turn = d.state.turnNumber

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.state.stack.size shouldBe 1 // the trigger
        val handBefore = d.getHand(d.player1).size
        d.bothPass() // resolve the trigger

        d.bothPass() // leave the postcombat main phase → the added beginning phase
        d.state.phase shouldBe Phase.BEGINNING
        d.state.activePlayerId shouldBe d.player1
        d.state.turnNumber shouldBe turn
        d.isTapped(land) shouldBe false

        d.passPriorityUntil(Step.END)
        d.getHand(d.player1).size shouldBe handBefore + 1
        d.state.activePlayerId shouldBe d.player1
        d.state.turnNumber shouldBe turn
    }

    test("enchanting an opponent triggers on their postcombat main, not yours") {
        val d = driver()
        d.enchant(d.player2)

        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)
        d.state.stack.size shouldBe 0 // your postcombat main: no trigger

        // Through to the opponent's postcombat main.
        var guard = 0
        while (!(d.state.activePlayerId == d.player2 && d.currentStep == Step.POSTCOMBAT_MAIN) && guard++ < 100) {
            d.bothPass()
        }
        d.state.stack.size shouldBe 1
        val handBefore = d.getHand(d.player2).size
        d.bothPass() // resolve the trigger
        d.bothPass() // → the added beginning phase
        d.state.phase shouldBe Phase.BEGINNING
        d.state.activePlayerId shouldBe d.player2

        d.passPriorityUntil(Step.END)
        d.getHand(d.player2).size shouldBe handBefore + 1
        d.state.activePlayerId shouldBe d.player2
    }
})
