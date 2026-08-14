package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

class ErietteOfTheCharmedAppleScenarioTest : ScenarioTestBase() {

    init {
        context("Eriette of the Charmed Apple") {
            test("a creature enchanted by your Aura cannot attack you") {
                val game = scenario()
                    .withPlayers("Eriette", "Opponent")
                    .withCardOnBattlefield(1, "Eriette of the Charmed Apple")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                val result = game.declareAttackers(mapOf("Grizzly Bears" to 1))

                withClue("Eriette prevents a creature carrying her controller's Aura from attacking that controller") {
                    result.error.shouldNotBeNull()
                }
            }

            test("an Aura controlled by the creature's controller does not prevent the attack") {
                val game = scenario()
                    .withPlayers("Eriette", "Opponent")
                    .withCardOnBattlefield(1, "Eriette of the Charmed Apple")
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(2, "Holy Strength", "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                withClue("Eriette only checks Auras controlled by Eriette's controller") {
                    game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                }
            }

            test("end step drains for the number of Auras controlled as the trigger resolves") {
                val game = scenario()
                    .withPlayers("Eriette", "Opponent")
                    .withCardOnBattlefield(1, "Eriette of the Charmed Apple")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                    .withCardAttachedTo(1, "Unholy Strength", "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                game.getLifeTotal(1) shouldBe 22
                game.getLifeTotal(2) shouldBe 18
            }
        }
    }
}
