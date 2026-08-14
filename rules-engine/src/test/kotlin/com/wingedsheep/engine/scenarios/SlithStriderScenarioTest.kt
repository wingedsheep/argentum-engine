package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Slith Strider (MRD #50, {1}{U}{U}, Creature — Slith 1/1).
 *
 *   Whenever this creature becomes blocked, draw a card.
 *   Whenever this creature deals combat damage to a player, put a +1/+1 counter on it.
 *
 * The two triggers are mutually exclusive in practice — getting blocked stops the damage that would
 * have grown it — so each test drives exactly one of them.
 */
class SlithStriderScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Slith Strider") {

            test("connecting with a player puts a +1/+1 counter on it") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Slith Strider", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val slith = game.findPermanent("Slith Strider")!!
                val handBefore = game.handSize(1)

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Slith Strider" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                withClue("Bob should have taken 1 combat damage") {
                    game.getLifeTotal(2) shouldBe 19
                }
                withClue("The +1/+1 counter turns the 1/1 into a 2/2") {
                    val after = stateProjector.project(game.state)
                    after.getPower(slith) shouldBe 2
                    after.getToughness(slith) shouldBe 2
                }
                withClue("Being unblocked draws nothing") {
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("becoming blocked draws a card and grows nothing") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Slith Strider", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val slith = game.findPermanent("Slith Strider")!!
                val handBefore = game.handSize(1)

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Slith Strider" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Slith Strider"))).error shouldBe null
                game.resolveStack()

                withClue("The becomes-blocked trigger draws exactly one card") {
                    game.handSize(1) shouldBe handBefore + 1
                }
                withClue("A blocked Slith is still a 1/1 — no combat damage reached a player") {
                    val after = stateProjector.project(game.state)
                    after.getPower(slith) shouldBe 1
                    after.getToughness(slith) shouldBe 1
                }
            }
        }
    }
}
