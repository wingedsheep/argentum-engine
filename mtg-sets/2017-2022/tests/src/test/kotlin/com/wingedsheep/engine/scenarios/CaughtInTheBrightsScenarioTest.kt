package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Caught in the Brights (AER #10) — {2}{W} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature can't attack or block.
 *   When a Vehicle you control attacks, exile enchanted creature.
 *
 * In every test Player1 controls the Aura, attached to Player2's Grizzly Bears.
 */
class CaughtInTheBrightsScenarioTest : ScenarioTestBase() {

    init {
        context("Caught in the Brights") {

            test("the enchanted creature can't attack") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withCardAttachedTo(1, "Caught in the Brights", "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldNotBe null
                withClue("control: an unenchanted creature can still attack in the same step") {
                    game.declareAttackers(mapOf("Savannah Lions" to 1)).error shouldBe null
                }
            }

            test("the enchanted creature can't block, and a non-Vehicle attacking doesn't trigger the exile") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardAttachedTo(1, "Caught in the Brights", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Savannah Lions" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                withClue("Savannah Lions is a creature, not a Vehicle — no trigger") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Savannah Lions"))).error shouldNotBe null
                withClue("control: an unenchanted creature can still block") {
                    game.declareBlockers(mapOf("Hill Giant" to listOf("Savannah Lions"))).error shouldBe null
                }
            }

            test("when a Vehicle you control attacks, the enchanted creature is exiled and the Aura goes to the graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Sleek Schooner")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardAttachedTo(1, "Caught in the Brights", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val schooner = game.findPermanent("Sleek Schooner")!!
                val lions = game.findPermanent("Savannah Lions")!!
                game.execute(CrewVehicle(game.player1Id, schooner, listOf(lions))).error shouldBe null
                game.resolveStack()
                game.state.projectedState.isCreature(schooner) shouldBe true

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Sleek Schooner" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the enchanted Grizzly Bears is exiled") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                }
                withClue("the Aura is put into its owner's graveyard") {
                    game.isInGraveyard(1, "Caught in the Brights") shouldBe true
                }
            }
        }
    }
}
