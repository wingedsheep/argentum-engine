package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Serendib Djinn (ARN #19).
 *
 * "{2}{U}{U} Creature — Djinn 5/6. Flying.
 *  At the beginning of your upkeep, sacrifice a land. If you sacrifice an Island this way,
 *  this creature deals 3 damage to you.
 *  When you control no lands, sacrifice this creature."
 *
 * Pins the composition over existing primitives (no new engine features):
 *  - The upkeep trigger sacrifices a land you choose (`Effects.Sacrifice(Land, Controller)`),
 *    and a `ConditionalEffect` gated on `SacrificedHadSubtype("Island")` reads the sacrificed
 *    permanent's snapshot to deal 3 damage to you only when the sacrificed land was an Island.
 *  - The "control no lands" clause is a state-triggered ability (CR 603.8) that sacrifices the
 *    Djinn, mirroring Dandân.
 */
class SerendibDjinnScenarioTest : ScenarioTestBase() {

    init {
        context("Serendib Djinn upkeep land sacrifice") {

            test("sacrificing an Island deals 3 damage to you") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Serendib Djinn", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                // Library fuel so neither player decks during the step advances.
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                // Advance to player 1's upkeep so the "your upkeep" trigger fires.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("Sacrifice prompt should name the actual filter (land), not 'creature'") {
                    game.getPendingDecision()?.prompt shouldBe "Choose a land to sacrifice"
                }

                // Two lands, sacrifice one — choose the Island.
                val island = game.findPermanent("Island")!!
                game.selectCards(listOf(island))
                game.resolveStack()

                withClue("Island should be in the graveyard") {
                    game.isInGraveyard(1, "Island") shouldBe true
                }
                withClue("Sacrificing an Island deals 3 damage: 20 - 3 = 17") {
                    game.getLifeTotal(1) shouldBe 17
                }
                withClue("Mountain remains, so the Djinn is not state-sacrificed") {
                    game.isOnBattlefield("Serendib Djinn") shouldBe true
                }
            }

            test("sacrificing a non-Island land deals no damage") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Serendib Djinn", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                // Choose the Mountain — not an Island, so no self-damage.
                val mountain = game.findPermanent("Mountain")!!
                game.selectCards(listOf(mountain))
                game.resolveStack()

                withClue("Mountain should be in the graveyard") {
                    game.isInGraveyard(1, "Mountain") shouldBe true
                }
                withClue("Sacrificing a non-Island land deals no damage") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }
        }

        context("Serendib Djinn state-triggered sacrifice") {

            test("is sacrificed when its controller controls no lands") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Serendib Djinn", summoningSickness = false)
                    // No lands controlled — the state trigger should fire.
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.resolveStack()

                withClue("With no lands, the state-triggered ability sacrifices the Djinn") {
                    game.isOnBattlefield("Serendib Djinn") shouldBe false
                }
            }
        }
    }
}
