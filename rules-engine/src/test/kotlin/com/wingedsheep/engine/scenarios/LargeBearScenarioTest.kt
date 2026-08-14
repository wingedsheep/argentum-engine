package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Large Bear (HOB #159) — {3}{B/G}{B/G} Creature — Bear 5/5 with reach, trample and haste.
 *
 * Each of the three keywords is exercised for what it actually does: reach blocks a flier,
 * trample pushes excess damage past a chump blocker, and haste is present on a creature that
 * still carries summoning sickness.
 */
class LargeBearScenarioTest : ScenarioTestBase() {

    init {
        context("Large Bear") {

            test("it is a 5/5 with reach, trample and haste") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Large Bear")
                    .build()

                val bear = game.findPermanent("Large Bear")!!
                val projected = game.state.projectedState
                projected.getPower(bear) shouldBe 5
                projected.getToughness(bear) shouldBe 5
                projected.hasKeyword(bear, Keyword.REACH) shouldBe true
                projected.hasKeyword(bear, Keyword.TRAMPLE) shouldBe true
                projected.hasKeyword(bear, Keyword.HASTE) shouldBe true
            }

            test("haste is present even while it is still summoning sick") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Large Bear", summoningSickness = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val bear = game.findPermanent("Large Bear")!!
                withClue("the creature really is summoning sick") {
                    game.state.getEntity(bear)?.has<SummoningSicknessComponent>() shouldBe true
                }
                withClue("haste is what lets it attack anyway") {
                    game.state.projectedState.hasKeyword(bear, Keyword.HASTE) shouldBe true
                    game.declareAttackers(mapOf("Large Bear" to 2)).error shouldBe null
                }
            }

            test("trample assigns damage past a 2/2 blocker to the defending player") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Large Bear")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Large Bear" to 2)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Large Bear"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
                game.resolveStack()
                if (game.hasPendingDecision()) {
                    game.submitDefaultCombatDamage()
                    game.resolveStack()
                }

                withClue("2 lethal to the blocker, 3 trampling over") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.getLifeTotal(2) shouldBe 17
                }
            }

            test("reach lets it block a flier") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    // Old Thrush is a 1/2 flier.
                    .withCardOnBattlefield(2, "Old Thrush")
                    .withCardOnBattlefield(1, "Large Bear")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Old Thrush" to 1)).error shouldBe null
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                val block = game.declareBlockers(mapOf("Large Bear" to listOf("Old Thrush")))
                withClue("reach can block flying: ${block.error}") { block.error shouldBe null }
            }
        }
    }
}
