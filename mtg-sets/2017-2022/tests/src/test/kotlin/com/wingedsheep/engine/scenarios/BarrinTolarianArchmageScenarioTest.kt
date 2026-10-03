package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Barrin, Tolarian Archmage (M21 #45) — ETB returns up to one other target creature or planeswalker
 * to its owner's hand; at the beginning of your end step, if a permanent was put into your hand from
 * the battlefield this turn, draw a card.
 */
class BarrinTolarianArchmageScenarioTest : ScenarioTestBase() {
    init {
        fun barrinGame() = scenario().withPlayers()
            .withCardInHand(1, "Barrin, Tolarian Archmage")
            .withCardInHand(1, "Unsummon")
            .withLandsOnBattlefield(1, "Island", 4)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.castBarrinTargeting(name: String?) {
            castSpell(1, "Barrin, Tolarian Archmage").error shouldBe null
            resolveStack()
            if (name == null) {
                skipTargets().error shouldBe null
            } else {
                selectTargets(listOf(findPermanent(name)!!)).error shouldBe null
            }
            resolveStack()
        }

        test("bouncing your own creature draws a card at your end step") {
            val game = barrinGame()
            game.castBarrinTargeting("Grizzly Bears")
            game.isInHand(1, "Grizzly Bears") shouldBe true

            val handBefore = game.handSize(1)
            game.passUntilPhase(Phase.ENDING, Step.END)
            withClue("the end-step trigger is on the stack") { game.state.stack.size shouldBe 1 }
            game.resolveStack()

            game.handSize(1) shouldBe handBefore + 1
        }

        test("bouncing an opponent's creature puts it in their hand, not yours — no draw") {
            val game = barrinGame()
            game.castBarrinTargeting("Hill Giant")
            game.isInHand(2, "Hill Giant") shouldBe true

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.state.stack shouldBe emptyList()
        }

        test("with no permanent returned the end-step ability doesn't trigger") {
            val game = barrinGame()
            game.castBarrinTargeting(null)

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.state.stack shouldBe emptyList()
        }

        test("a bounce earlier in the turn, before Barrin entered, still counts") {
            val game = barrinGame()
            game.castSpell(1, "Unsummon", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.castBarrinTargeting(null)

            val handBefore = game.handSize(1)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            game.handSize(1) shouldBe handBefore + 1
        }

        test("the record resets at end of turn") {
            val game = barrinGame()
            game.castBarrinTargeting("Grizzly Bears")
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            // Next turn is the opponent's; then back to ours with no bounce — no trigger.
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.activePlayerId shouldBe game.player1Id
            game.passUntilPhase(Phase.ENDING, Step.END)
            game.state.stack shouldBe emptyList()
        }
    }
}
