package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Arm the Cathars (VOW #3) — {1}{W}{W} Sorcery.
 *
 *   Until end of turn, target creature gets +3/+3, up to one other target creature gets +2/+2,
 *   and up to one other target creature gets +1/+1. Those creatures gain vigilance until end
 *   of turn.
 *
 * Exercises the three-target pump (primary +3/+3, second +2/+2, third +1/+1), each paired with a
 * vigilance grant, and confirms an untargeted (skipped optional) creature is unaffected.
 */
class ArmTheCatharsScenarioTest : ScenarioTestBase() {

    init {
        context("Arm the Cathars") {

            test("targeting all three creatures pumps each and grants vigilance") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Arm the Cathars")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false) // 2/2 primary
                    .withCardOnBattlefield(1, "Savannah Lions", summoningSickness = false) // 1/1 second
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false) // 3/3 third
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val lions = game.findPermanent("Savannah Lions")!!
                val giant = game.findPermanent("Hill Giant")!!

                val card = game.findCardsInHand(1, "Arm the Cathars").first()
                game.execute(
                    CastSpell(
                        game.player1Id,
                        card,
                        listOf(
                            ChosenTarget.Permanent(bears),
                            ChosenTarget.Permanent(lions),
                            ChosenTarget.Permanent(giant),
                        ),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("primary target (Grizzly Bears) gets +3/+3 (becomes 5/5)") {
                    game.state.projectedState.getPower(bears) shouldBe 5
                    game.state.projectedState.getToughness(bears) shouldBe 5
                }
                withClue("second target (Savannah Lions) gets +2/+2 (becomes 3/3)") {
                    game.state.projectedState.getPower(lions) shouldBe 3
                    game.state.projectedState.getToughness(lions) shouldBe 3
                }
                withClue("third target (Hill Giant) gets +1/+1 (becomes 4/4)") {
                    game.state.projectedState.getPower(giant) shouldBe 4
                    game.state.projectedState.getToughness(giant) shouldBe 4
                }
                withClue("all three gain vigilance") {
                    game.state.projectedState.hasKeyword(bears, Keyword.VIGILANCE) shouldBe true
                    game.state.projectedState.hasKeyword(lions, Keyword.VIGILANCE) shouldBe true
                    game.state.projectedState.hasKeyword(giant, Keyword.VIGILANCE) shouldBe true
                }
            }

            test("skipping the optional targets leaves only the primary target affected") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Arm the Cathars")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                val card = game.findCardsInHand(1, "Arm the Cathars").first()
                game.execute(
                    CastSpell(game.player1Id, card, listOf(ChosenTarget.Permanent(bears)))
                ).error shouldBe null
                game.resolveStack()

                withClue("primary target still gets +3/+3 (becomes 5/5)") {
                    game.state.projectedState.getPower(bears) shouldBe 5
                    game.state.projectedState.getToughness(bears) shouldBe 5
                }
                withClue("untargeted creature is unaffected") {
                    game.state.projectedState.getPower(giant) shouldBe 3
                    game.state.projectedState.getToughness(giant) shouldBe 3
                    game.state.projectedState.hasKeyword(giant, Keyword.VIGILANCE) shouldBe false
                }
            }
        }
    }
}
