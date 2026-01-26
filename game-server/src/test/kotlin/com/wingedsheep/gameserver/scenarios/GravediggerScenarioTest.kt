package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Gravedigger's ETB triggered ability.
 *
 * Card reference:
 * - Gravedigger (3B): 2/2 Creature - Zombie
 *   "When Gravedigger enters the battlefield, you may return target creature card
 *   from your graveyard to your hand."
 */
class GravediggerScenarioTest : ScenarioTestBase() {

    init {
        context("Gravedigger ETB trigger") {
            test("returns creature card from graveyard to hand when target is selected") {
                // Setup: Player 1 has Gravedigger in hand and Grizzly Bears in graveyard
                val game = scenario()
                    .withPlayers("Gravedigger Player", "Opponent")
                    .withCardInHand(1, "Gravedigger")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Verify initial state
                withClue("Grizzly Bears should start in graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("Initial hand size should be 1 (just Gravedigger)") {
                    game.handSize(1) shouldBe 1
                }

                // Cast Gravedigger
                val castResult = game.castSpell(1, "Gravedigger")
                withClue("Gravedigger should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve the spell to put Gravedigger on battlefield
                game.resolveStack()

                // There should be a pending decision for target selection (ETB trigger)
                withClue("There should be a pending decision for target selection") {
                    game.hasPendingDecision() shouldBe true
                }

                // Find the Grizzly Bears in the graveyard
                val bearsInGraveyard = game.findCardsInGraveyard(1, "Grizzly Bears")
                withClue("Grizzly Bears should be found in graveyard") {
                    bearsInGraveyard.isNotEmpty() shouldBe true
                }

                // Select Grizzly Bears as the target to return
                val targetResult = game.selectTargets(bearsInGraveyard)
                withClue("Target selection should succeed") {
                    targetResult.error shouldBe null
                }

                // Resolve the triggered ability on the stack
                game.resolveStack()

                // Verify results
                withClue("Gravedigger should be on the battlefield") {
                    game.isOnBattlefield("Gravedigger") shouldBe true
                }

                withClue("Grizzly Bears should now be in hand (returned from graveyard)") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }

                withClue("Grizzly Bears should NOT be in graveyard anymore") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }

                withClue("Hand size should be 1 (Grizzly Bears returned)") {
                    game.handSize(1) shouldBe 1
                }

                withClue("Graveyard should be empty") {
                    game.graveyardSize(1) shouldBe 0
                }
            }

            test("ability is optional - player can skip target selection") {
                // Setup: Player 1 has Gravedigger in hand and a creature in graveyard
                val game = scenario()
                    .withPlayers("Gravedigger Player", "Opponent")
                    .withCardInHand(1, "Gravedigger")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast and resolve Gravedigger
                game.castSpell(1, "Gravedigger")
                game.resolveStack()

                withClue("There should be a pending decision for target selection") {
                    game.hasPendingDecision() shouldBe true
                }

                // Skip the target selection (decline the optional ability)
                game.skipTargets()

                // Resolve any remaining items
                game.resolveStack()

                // Verify results
                withClue("Gravedigger should be on the battlefield") {
                    game.isOnBattlefield("Gravedigger") shouldBe true
                }

                withClue("Grizzly Bears should still be in graveyard (not returned)") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }

                withClue("Hand should be empty (Gravedigger was cast, nothing returned)") {
                    game.handSize(1) shouldBe 0
                }
            }

            test("works with multiple creatures in graveyard - can choose which one") {
                // Setup: Player 1 has Gravedigger in hand and multiple creatures in graveyard
                val game = scenario()
                    .withPlayers("Gravedigger Player", "Opponent")
                    .withCardInHand(1, "Gravedigger")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Verify initial state
                withClue("Graveyard should have 2 creatures") {
                    game.graveyardSize(1) shouldBe 2
                }

                // Cast and resolve Gravedigger
                game.castSpell(1, "Gravedigger")
                game.resolveStack()

                withClue("There should be a pending decision for target selection") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select Hill Giant specifically (not Grizzly Bears)
                val hillGiantInGraveyard = game.findCardsInGraveyard(1, "Hill Giant")
                game.selectTargets(hillGiantInGraveyard)
                game.resolveStack()

                // Verify results
                withClue("Hill Giant should now be in hand") {
                    game.isInHand(1, "Hill Giant") shouldBe true
                }

                withClue("Grizzly Bears should still be in graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }

                withClue("Graveyard should have 1 creature remaining") {
                    game.graveyardSize(1) shouldBe 1
                }
            }

            test("only targets creature cards - non-creatures in graveyard are not valid") {
                // Setup: Player 1 has Gravedigger and only non-creature cards in graveyard
                val game = scenario()
                    .withPlayers("Gravedigger Player", "Opponent")
                    .withCardInHand(1, "Gravedigger")
                    .withCardInGraveyard(1, "Forest")  // Land - not a creature
                    .withCardInGraveyard(1, "Grizzly Bears")  // Creature - valid target
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast and resolve Gravedigger
                game.castSpell(1, "Gravedigger")
                game.resolveStack()

                withClue("There should be a pending decision for target selection") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select the creature card
                val bearsInGraveyard = game.findCardsInGraveyard(1, "Grizzly Bears")
                game.selectTargets(bearsInGraveyard)
                game.resolveStack()

                // Verify only creature was returned
                withClue("Grizzly Bears should be in hand") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }

                withClue("Forest should still be in graveyard (not a valid target)") {
                    game.isInGraveyard(1, "Forest") shouldBe true
                }
            }
        }
    }
}
