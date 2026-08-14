package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Fateful Discovery (HOB #40) — {3}{U}{U} Enchantment.
 * "Whenever an artifact you control enters, draw a card."
 *
 * Two axes have to hold: the entering permanent must be an *artifact* (a creature entering does
 * nothing), and it must be one *you* control (an opponent's artifact does nothing).
 */
class FatefulDiscoveryScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(CardDefinition.artifact("Test Trinket", ManaCost.parse("{1}")))

        context("Fateful Discovery") {

            test("an artifact you control entering draws a card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Fateful Discovery")
                    .withCardInHand(1, "Test Trinket")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.librarySize(1)
                game.castSpell(1, "Test Trinket").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the artifact resolved and the trigger drew one card") {
                    game.isOnBattlefield("Test Trinket") shouldBe true
                    game.librarySize(1) shouldBe libraryBefore - 1
                    game.handSize(1) shouldBe 1
                }
            }

            test("a creature you control entering does not trigger it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Fateful Discovery")
                    .withCardInHand(1, "Centaur Courser")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.librarySize(1)
                game.castSpell(1, "Centaur Courser").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the trigger reads 'an artifact', not 'a permanent'") {
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                    game.librarySize(1) shouldBe libraryBefore
                    game.handSize(1) shouldBe 0
                }
            }

            test("an opponent's artifact entering does not trigger it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Fateful Discovery")
                    .withCardInHand(2, "Test Trinket")
                    .withLandsOnBattlefield(2, "Island", 1)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.librarySize(1)
                game.castSpell(2, "Test Trinket").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the trigger is scoped to artifacts *you* control") {
                    game.isOnBattlefield("Test Trinket") shouldBe true
                    game.librarySize(1) shouldBe libraryBefore
                    game.handSize(1) shouldBe 0
                }
            }
        }
    }
}
