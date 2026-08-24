package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Deep Spawn (Fallen Empires).
 *
 * "At the beginning of your upkeep, sacrifice this creature unless you mill two cards."
 *
 * This is the only printed pay-or-suffer cost in the corpus that mills, and the branch was
 * unimplemented — the trigger raised an engine error and did nothing at all, so the Spawn neither
 * milled nor died. The third case is the rules-interesting one: CR 701.17b forbids paying a cost
 * that mills more cards than the library holds, so a one-card library can't pay and the sacrifice
 * happens with no prompt offered.
 */
class DeepSpawnScenarioTest : ScenarioTestBase() {

    init {
        context("Deep Spawn") {

            test("milling two cards keeps the Spawn on the battlefield") {
                val game = scenario()
                    .withPlayers("Spawner", "Opponent")
                    .withCardOnBattlefield(1, "Deep Spawn")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                game.resolveStack()

                withClue("the upkeep trigger must actually ask") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)
                game.checkStateBasedActions()

                withClue("paid the cost, so the Spawn lives") {
                    game.isOnBattlefield("Deep Spawn") shouldBe true
                }
                withClue("two cards milled into the graveyard") {
                    game.graveyardSize(1) shouldBe 2
                }
            }

            test("declining the mill sacrifices the Spawn") {
                val game = scenario()
                    .withPlayers("Spawner", "Opponent")
                    .withCardOnBattlefield(1, "Deep Spawn")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                game.answerYesNo(false)
                game.checkStateBasedActions()

                withClue("declined, so the Spawn is sacrificed") {
                    game.isOnBattlefield("Deep Spawn") shouldBe false
                    game.isInGraveyard(1, "Deep Spawn") shouldBe true
                }
                withClue("nothing was milled") {
                    game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 0
                }
            }

            test("a library too shallow to mill two can't pay, so the Spawn is sacrificed") {
                val game = scenario()
                    .withPlayers("Spawner", "Opponent")
                    .withCardOnBattlefield(1, "Deep Spawn")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.ENDING, Step.END)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                game.checkStateBasedActions()

                withClue("CR 701.17b — an unpayable cost is not offered") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("so the suffer half happens") {
                    game.isOnBattlefield("Deep Spawn") shouldBe false
                    game.isInGraveyard(1, "Deep Spawn") shouldBe true
                }
                withClue("and the one card left in the library stays there") {
                    game.findCardsInGraveyard(1, "Grizzly Bears").size shouldBe 0
                }
            }
        }
    }
}
