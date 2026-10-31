package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Shinechaser (ELD #201) — {1}{W}{U} Creature — Faerie 1/1.
 *
 * "Flying, vigilance / This creature gets +1/+1 as long as you control an artifact. / This
 * creature gets +1/+1 as long as you control an enchantment."
 */
class ShinechaserScenarioTest : ScenarioTestBase() {

    private fun board(vararg extras: String): TestGame {
        var builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Shinechaser")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        for (extra in extras) builder = builder.withCardOnBattlefield(1, extra)
        return builder.build()
    }

    private fun TestGame.stats(): Pair<Int?, Int?> {
        val id = findPermanent("Shinechaser")!!
        return state.projectedState.getPower(id) to state.projectedState.getToughness(id)
    }

    init {
        context("Shinechaser") {

            test("is a 1/1 with neither an artifact nor an enchantment") {
                board().stats() shouldBe (1 to 1)
            }

            test("gets +1/+1 for an artifact") {
                board("Arcane Signet").stats() shouldBe (2 to 2)
            }

            test("gets +1/+1 for an enchantment") {
                board("Propaganda").stats() shouldBe (2 to 2)
            }

            test("gets +2/+2 with both an artifact and an enchantment") {
                withClue("the two bonuses are independent and stack") {
                    board("Arcane Signet", "Propaganda").stats() shouldBe (3 to 3)
                }
            }

            test("an opponent's artifact doesn't count") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Shinechaser")
                    .withCardOnBattlefield(2, "Arcane Signet")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.stats() shouldBe (1 to 1)
            }
        }
    }
}
