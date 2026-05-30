package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Loafing Giant.
 *
 * Card reference:
 * - Loafing Giant ({4}{R}): Creature — Giant 4/6
 *   Whenever this creature attacks or blocks, mill a card. If a land card was milled this
 *   way, prevent all combat damage this creature would deal this turn.
 *
 * Verifies the milled-card-type branch: a land milled prevents Loafing Giant's combat
 * damage, while a nonland milled lets the damage through.
 */
class LoafingGiantScenarioTest : ScenarioTestBase() {

    init {
        context("Loafing Giant attack trigger") {

            test("milling a land prevents Loafing Giant's combat damage") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Loafing Giant", tapped = false)
                    .withCardInLibrary(1, "Forest")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Loafing Giant" to 2))
                // Resolve the "attacks" trigger: mills the Forest, then prevents damage.
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Forest should have been milled to the graveyard") {
                    game.findCardsInGraveyard(1, "Forest").size shouldBe 1
                }
                withClue("Combat damage should be prevented when a land is milled") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("milling a nonland deals combat damage normally") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Loafing Giant", tapped = false)
                    .withCardInLibrary(1, "Kavu Aggressor")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Loafing Giant" to 2))
                // Resolve the "attacks" trigger: mills a nonland, no prevention.
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Kavu Aggressor should have been milled to the graveyard") {
                    game.findCardsInGraveyard(1, "Kavu Aggressor").size shouldBe 1
                }
                withClue("Loafing Giant (4 power) should deal combat damage when a nonland is milled") {
                    game.getLifeTotal(2) shouldBe 16
                }
            }
        }
    }
}
