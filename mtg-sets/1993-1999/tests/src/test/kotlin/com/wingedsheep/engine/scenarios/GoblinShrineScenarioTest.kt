package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Goblin Shrine — the conditional +1/+0 lord plus "When this Aura leaves the
 * battlefield, it deals 1 damage to each Goblin creature."
 *
 * The parting shot is the interesting half, and the case that proves it is the self-inflicted one:
 * destroy the enchanted land, and the Shrine falls off and kills the very 1/1 Goblins it was
 * pumping. A trigger wired to "dies" rather than "leaves the battlefield" would still pass a
 * disenchant test but not this one, and one whose group had drifted to "Goblins you control" would
 * spare the opponent's.
 */
class GoblinShrineScenarioTest : ScenarioTestBase() {

    init {
        context("Goblin Shrine") {

            test("Goblins get +1/+0 while it enchants a basic Mountain") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardAttachedTo(1, "Goblin Shrine", "Mountain")
                    .withCardOnBattlefield(1, "Goblin Balloon Brigade")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val goblin = game.findPermanent("Goblin Balloon Brigade")!!
                val bear = game.findPermanent("Grizzly Bears")!!

                withClue("1/1 becomes 2/1") {
                    game.state.projectedState.getPower(goblin) shouldBe 2
                    game.state.projectedState.getToughness(goblin) shouldBe 1
                }
                withClue("non-Goblins are untouched") {
                    game.state.projectedState.getPower(bear) shouldBe 2
                }
            }

            test("no anthem when the enchanted land isn't a basic Mountain") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Forest")
                    .withCardAttachedTo(1, "Goblin Shrine", "Forest")
                    .withCardOnBattlefield(1, "Goblin Balloon Brigade")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.state.projectedState.getPower(game.findPermanent("Goblin Balloon Brigade")!!) shouldBe 1
            }

            test("leaving the battlefield burns every Goblin, on both sides") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardAttachedTo(1, "Goblin Shrine", "Mountain")
                    .withCardOnBattlefield(1, "Goblin Balloon Brigade")
                    .withCardOnBattlefield(2, "Marsh Goblins")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInHand(1, "Stone Rain")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Destroying the host takes the Aura with it — the Shrine's own anthem is what
                // kept these Goblins alive at 1 toughness, and it doesn't survive its departure.
                // Ask the Shrine what it is attached to — several Mountains are on the board and
                // only one of them is the host.
                val shrine = game.findPermanent("Goblin Shrine")!!
                val enchanted = game.state.getEntity(shrine)!!.get<AttachedToComponent>()!!.targetId
                game.castSpell(1, "Stone Rain", enchanted).error shouldBe null
                game.resolveStack()

                withClue("the Shrine left the battlefield with its host") {
                    game.isInGraveyard(1, "Goblin Shrine") shouldBe true
                }
                withClue("both 1/1 Goblins took 1 and died") {
                    game.isInGraveyard(1, "Goblin Balloon Brigade") shouldBe true
                    game.isInGraveyard(2, "Marsh Goblins") shouldBe true
                }
                withClue("the non-Goblin was never a target of the trigger") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
