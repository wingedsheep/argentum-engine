package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Angry Mob — a CDA that reads one value on your turn and a flat 2 on anyone
 * else's.
 *
 * Three ways this goes wrong, one case each: the turn clause ignored (so the Mob would stay big on
 * the opponent's turn), the controller clause inverted (your own Swamps counting), and the count
 * reading basics only rather than the Swamp subtype.
 */
class AngryMobScenarioTest : ScenarioTestBase() {

    init {
        context("Angry Mob") {

            test("on your turn it is 2 plus the Swamps your opponents control") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Angry Mob")
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mob = game.findPermanent("Angry Mob")!!
                withClue("2 + 3 Swamps") {
                    game.state.projectedState.getPower(mob) shouldBe 5
                    game.state.projectedState.getToughness(mob) shouldBe 5
                }
            }

            test("on someone else's turn it is a flat 2 however many Swamps are out") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Angry Mob")
                    .withLandsOnBattlefield(2, "Swamp", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mob = game.findPermanent("Angry Mob")!!
                game.state.projectedState.getPower(mob) shouldBe 2
                game.state.projectedState.getToughness(mob) shouldBe 2
            }

            test("your own Swamps don't feed it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Angry Mob")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val mob = game.findPermanent("Angry Mob")!!
                withClue("the card says Swamps your opponents control") {
                    game.state.projectedState.getPower(mob) shouldBe 2
                }
            }
        }
    }
}
