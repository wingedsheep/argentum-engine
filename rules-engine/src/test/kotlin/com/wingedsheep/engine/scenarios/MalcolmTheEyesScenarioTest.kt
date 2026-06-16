package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Malcolm, the Eyes (OTJ #219, {U}{R} 2/2 Flying, haste).
 *
 *   Whenever you cast your second spell each turn, investigate.
 *
 * Exercises the new `Effects.Investigate()` / Clue predefined token via the
 * `Triggers.NthSpellCast(2, You)` trigger.
 */
class MalcolmTheEyesScenarioTest : ScenarioTestBase() {

    init {
        context("Malcolm, the Eyes") {

            test("casting your second spell each turn creates a Clue token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Malcolm, the Eyes")
                    .withCardsInHand(1, "Lightning Bolt", 2)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                fun clueCount() = game.state.getBattlefield().count { id ->
                    game.state.getEntity(id)?.get<CardComponent>()?.name == "Clue"
                }

                // First spell — no investigate.
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                game.resolveStack()
                clueCount() shouldBe 0

                // Second spell — investigate.
                game.castSpellTargetingPlayer(1, "Lightning Bolt", 2)
                game.resolveStack()

                withClue("Casting the second spell investigates (one Clue token)") {
                    clueCount() shouldBe 1
                }
            }
        }
    }
}
