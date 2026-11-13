package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Exert Influence (BFZ #77) — {4}{U} Sorcery. "Converge — Gain control of target creature if its
 * power is less than or equal to the number of colors of mana spent to cast this spell."
 *
 * Any creature can be targeted; the power is compared on resolution against the colors spent, so
 * the converge count has to reach the condition while the spell resolves.
 */
class ExertInfluenceScenarioTest : ScenarioTestBase() {
    init {
        context("Exert Influence — converge-gated steal") {
            test("three colors spent steals a 3-power creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Exert Influence")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!
                game.castSpell(1, "Exert Influence", giant).error shouldBe null
                game.resolveStack()

                withClue("U, R and G were spent: power 3 <= 3, so the Giant changes hands") {
                    game.state.projectedState.getController(giant) shouldBe game.player1Id
                }
            }

            test("one color spent leaves a 2-power creature with its controller") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Exert Influence")
                    .withLandsOnBattlefield(1, "Island", 5)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.castSpell(1, "Exert Influence", bears).error shouldBe null
                game.resolveStack()

                withClue("Only blue was spent: power 2 > 1, so the spell resolves with no effect") {
                    game.state.projectedState.getController(bears) shouldBe game.player2Id
                    game.isInGraveyard(1, "Exert Influence") shouldBe true
                }
            }
        }
    }
}
