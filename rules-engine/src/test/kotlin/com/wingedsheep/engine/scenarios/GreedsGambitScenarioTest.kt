package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Greed's Gambit (BIG #8) — {3}{B} Enchantment.
 *
 * "When this enchantment enters, you draw three cards, gain 6 life, and create three 2/1 black
 *  Bat creature tokens with flying.
 *  At the beginning of your end step, you discard a card, lose 2 life, and sacrifice a creature.
 *  When this enchantment leaves the battlefield, you discard three cards, lose 6 life, and
 *  sacrifice three creatures."
 */
class GreedsGambitScenarioTest : ScenarioTestBase() {

    init {
        test("ETB draws three, gains 6 life, and creates three flying Bats") {
            val game = scenario()
                .withPlayers()
                .withCardInHand(1, "Greed's Gambit")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .build()

            val lifeBefore = game.getLifeTotal(1)
            game.castSpell(1, "Greed's Gambit").error shouldBe null
            game.resolveStack()

            withClue("gained 6 life") {
                game.getLifeTotal(1) shouldBe lifeBefore + 6
            }
            withClue("created three Bat tokens") {
                batCount(game) shouldBe 3
            }
        }

        test("end step: discard, lose 2 life, sacrifice a creature") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Greed's Gambit")
                .withCardInHand(1, "Swamp")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .build()

            val lifeBefore = game.getLifeTotal(1)
            game.passUntilPhase(Phase.ENDING, Step.END)
            // The "at the beginning of your end step" trigger is queued; resolve it.
            game.resolveStack()
            game.resolveStack()

            withClue("lost 2 life at end step") {
                game.getLifeTotal(1) shouldBe lifeBefore - 2
            }
            withClue("the single creature was sacrificed") {
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }
            withClue("the single card in hand was discarded") {
                game.handSize(1) shouldBe 0
            }
        }

        test("leaves the battlefield: discard three, lose 6 life, sacrifice three creatures") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Greed's Gambit")
                .withCardInHand(1, "Swamp")
                .withCardInHand(1, "Swamp")
                .withCardInHand(1, "Swamp")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Grizzly Bears")
                // The active player destroys their own enchantment to drive the leaves trigger.
                .withCardInHand(1, "Naturalize")
                .withLandsOnBattlefield(1, "Forest", 2)
                .build()

            val lifeBefore = game.getLifeTotal(1)

            val gambit = game.findPermanent("Greed's Gambit")!!
            game.castSpell(1, "Naturalize", gambit).error shouldBe null
            game.resolveStack() // Naturalize resolves → Greed's Gambit leaves → its trigger goes on the stack
            game.resolveStack() // resolve the leaves-the-battlefield trigger

            withClue("Greed's Gambit was destroyed") {
                game.isOnBattlefield("Greed's Gambit") shouldBe false
            }
            withClue("lost 6 life when it left the battlefield") {
                game.getLifeTotal(1) shouldBe lifeBefore - 6
            }
            withClue("all three creatures were sacrificed") {
                game.findPermanents("Grizzly Bears").size shouldBe 0
            }
            withClue("all three cards were discarded") {
                game.handSize(1) shouldBe 0
            }
        }
    }

    private fun batCount(game: TestGame): Int =
        game.state.getBattlefield(game.player1Id).count { id ->
            game.state.getEntity(id)?.get<CardComponent>()
                ?.typeLine?.subtypes?.any { it.value == "Bat" } == true
        }
}
