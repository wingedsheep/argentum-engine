package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Jace's Scrutiny (SOI #70) — {1}{U} Instant.
 *
 * "Target creature gets -4/-0 until end of turn. Investigate."
 *
 * Covers the -4/-0 (power only, wearing off at end of turn), the Clue, and the ruling that an
 * illegal target on resolution means no investigate.
 */
class JacesScrutinyScenarioTest : ScenarioTestBase() {

    private fun board(): TestGame = scenario()
        .withPlayers("Caster", "Opponent")
        .withCardInHand(1, "Jace's Scrutiny")
        .withCardInHand(1, "Lightning Bolt")
        .withCardOnBattlefield(2, "Hill Giant")
        .withLandsOnBattlefield(1, "Island", 2)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("target creature gets -4/-0 until end of turn and you investigate") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Jace's Scrutiny", giant).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            withClue("Hill Giant 3/3 gets -4/-0 -> power -1") {
                projected.getPower(giant) shouldBe -1
            }
            withClue("toughness is untouched") {
                projected.getToughness(giant) shouldBe 3
            }
            withClue("one Clue was created under the caster's control") {
                game.findPermanents("Clue").size shouldBe 1
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            withClue("still -4/-0 during the end step") {
                game.state.projectedState.getPower(giant) shouldBe -1
            }
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("the -4/-0 wears off at end of turn") {
                game.state.projectedState.getPower(giant) shouldBe 3
            }
        }

        test("if the target becomes illegal, the spell doesn't resolve and there is no Clue") {
            val game = board()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Jace's Scrutiny", giant).error shouldBe null
            game.castSpell(1, "Lightning Bolt", giant).error shouldBe null
            game.resolveStack()

            withClue("Hill Giant died to Lightning Bolt before Jace's Scrutiny resolved") {
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
            withClue("the spell was countered on resolution — no investigate") {
                game.findPermanents("Clue").size shouldBe 0
            }
            game.isInGraveyard(1, "Jace's Scrutiny") shouldBe true
        }
    }
}
