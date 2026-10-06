package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Engulfing Slagwurm.
 *
 * The interesting part is the life gain: it reads the destroyed creature's toughness *as it last
 * existed on the battlefield*. The blockers here are pumped by the opponent's Glorious Anthem, so
 * a read taken after the destroy (printed toughness from the graveyard) would gain 5 instead of 7.
 * Two blockers also prove the trigger fires once per partner, each bound to its own creature.
 */
class EngulfingSlagwurmScenarioTest : ScenarioTestBase() {

    private fun TestGame.resolveAll() {
        repeat(10) {
            val decision = state.pendingDecision
            if (decision is OrderObjectsDecision) {
                submitDecision(OrderedResponse(decision.id, decision.objects))
            }
            resolveStack()
        }
    }

    init {
        context("Engulfing Slagwurm") {

            test("becomes blocked by two creatures: destroys each, gains their last-known toughness") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Engulfing Slagwurm", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(2, "Glorious Anthem")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Engulfing Slagwurm" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(
                    mapOf(
                        "Grizzly Bears" to listOf("Engulfing Slagwurm"),
                        "Hill Giant" to listOf("Engulfing Slagwurm"),
                    )
                ).error shouldBe null
                game.resolveAll()

                withClue("both blockers destroyed") {
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
                withClue("3 + 4 (anthem-pumped), not the printed 2 + 3") {
                    game.getLifeTotal(1) shouldBe 27
                }
            }

            test("blocks a creature: destroys it and gains its toughness") {
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(1, "Engulfing Slagwurm")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Engulfing Slagwurm" to listOf("Hill Giant"))).error shouldBe null
                game.resolveAll()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.getLifeTotal(1) shouldBe 23
            }
        }
    }
}
