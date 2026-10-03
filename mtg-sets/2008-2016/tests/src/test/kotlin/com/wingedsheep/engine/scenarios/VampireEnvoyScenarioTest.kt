package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Vampire Envoy ({2}{B}, 1/4 flier): "Whenever this creature becomes tapped, you gain 1 life."
 *
 * Attacking taps it, which fires the `Triggers.self.becomesTapped()` trigger.
 */
class VampireEnvoyScenarioTest : ScenarioTestBase() {

    init {
        context("Vampire Envoy") {

            test("attacking taps it and its controller gains 1 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vampire Envoy", summoningSickness = false)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Vampire Envoy" to 2)).error shouldBe null
                game.resolveStack()

                withClue("becoming tapped by attacking gains 1 life") {
                    game.getLifeTotal(1) shouldBe 21
                }
                withClue("the opponent gains nothing") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("no life is gained while it stays untapped") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Vampire Envoy", summoningSickness = false)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(emptyMap()).error shouldBe null
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
