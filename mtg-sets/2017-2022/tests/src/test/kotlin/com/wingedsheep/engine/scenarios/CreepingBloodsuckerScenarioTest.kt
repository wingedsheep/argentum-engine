package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Creeping Bloodsucker (J22 #21) — {1}{B} Creature — Vampire, 1/2.
 *
 * "At the beginning of your upkeep, this creature deals 1 damage to each opponent. You gain life
 *  equal to the damage dealt this way."
 *
 * The gain reads the damage actually dealt: it still happens when the Bloodsucker is gone by the
 * time the trigger resolves, and nothing is gained when the damage is prevented.
 */
class CreepingBloodsuckerScenarioTest : ScenarioTestBase() {

    private fun upkeepWithTriggerOnStack(bobHand: String? = null): TestGame {
        val builder = scenario()
            .withPlayers("Alice", "Bob")
            .withCardOnBattlefield(1, "Creeping Bloodsucker")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(2)
            .inPhase(Phase.ENDING, Step.END)
        if (bobHand != null) {
            builder.withCardInHand(2, bobHand)
            builder.withLandsOnBattlefield(2, "Plains", 3)
            builder.withLandsOnBattlefield(2, "Swamp", 2)
        }
        val game = builder.build()
        game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
        game.state.activePlayerId shouldBe game.player1Id
        game.state.stack.size shouldBe 1
        return game
    }

    init {
        context("Creeping Bloodsucker") {

            test("upkeep: deals 1 damage to the opponent and its controller gains 1 life") {
                val game = upkeepWithTriggerOnStack()
                game.resolveStack()

                game.getLifeTotal(2) shouldBe 19
                game.getLifeTotal(1) shouldBe 21
            }

            test("still drains when the Bloodsucker has left the battlefield before the trigger resolves") {
                val game = upkeepWithTriggerOnStack(bobHand = "Murder")
                val bloodsucker = game.findPermanent("Creeping Bloodsucker")!!

                game.passPriority().error shouldBe null
                game.castSpell(2, "Murder", bloodsucker).error shouldBe null
                game.resolveStack()

                withClue("Murder resolved first") { game.isInGraveyard(1, "Creeping Bloodsucker") shouldBe true }
                withClue("the damage is dealt from last known information, and its amount is gained") {
                    game.getLifeTotal(2) shouldBe 19
                    game.getLifeTotal(1) shouldBe 21
                }
            }

            test("prevented damage gains no life") {
                val game = upkeepWithTriggerOnStack(bobHand = "Safe Passage")

                game.passPriority().error shouldBe null
                game.castSpell(2, "Safe Passage").error shouldBe null
                game.resolveStack()

                withClue("Safe Passage prevented the damage to Bob, so no damage was dealt this way") {
                    game.getLifeTotal(2) shouldBe 20
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }
    }
}
