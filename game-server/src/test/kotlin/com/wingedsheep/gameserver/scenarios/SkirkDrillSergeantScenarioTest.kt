package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class SkirkDrillSergeantScenarioTest : ScenarioTestBase() {

    init {
        context("Skirk Drill Sergeant") {
            test("reveals Goblin permanent and puts it onto battlefield when another Goblin dies") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Skirk Drill Sergeant")
                    .withCardOnBattlefield(1, "Goblin Grappler") // 1/1 Goblin to kill
                    .withLandsOnBattlefield(1, "Mountain", 3) // For {2}{R} may pay
                    .withCardInLibrary(1, "Goblin Goon") // Goblin permanent on top
                    .withCardInHand(2, "Shock") // To kill the Goblin Grappler
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Opponent casts Shock targeting Goblin Grappler
                val grapplerId = game.findPermanent("Goblin Grappler")!!
                game.castSpell(2, "Shock", grapplerId)
                game.resolveStack() // Resolve Shock, Grappler dies

                // Skirk Drill Sergeant triggers — "you may pay {2}{R}"
                game.hasPendingDecision() shouldBe true
                game.answerYesNo(true)
                game.submitManaSourcesAutoPay()

                // Resolve the triggered ability
                game.resolveStack()

                // Goblin Goon (Goblin creature) should be on the battlefield
                game.isOnBattlefield("Goblin Goon") shouldBe true
            }

            test("reveals non-Goblin card and puts it into graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Skirk Drill Sergeant")
                    .withCardOnBattlefield(1, "Goblin Grappler")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Glory Seeker") // Non-Goblin creature on top
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val grapplerId = game.findPermanent("Goblin Grappler")!!
                game.castSpell(2, "Shock", grapplerId)
                game.resolveStack()

                game.answerYesNo(true)
                game.submitManaSourcesAutoPay()
                game.resolveStack()

                // Glory Seeker is not a Goblin, should go to graveyard not battlefield
                game.isInGraveyard(1, "Glory Seeker") shouldBe true
                game.isOnBattlefield("Glory Seeker") shouldBe false
            }

            test("declining to pay leaves library intact") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Skirk Drill Sergeant")
                    .withCardOnBattlefield(1, "Goblin Grappler")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Goblin Goon")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.librarySize(1)
                val grapplerId = game.findPermanent("Goblin Grappler")!!
                game.castSpell(2, "Shock", grapplerId)
                game.resolveStack()

                // Decline to pay
                game.answerYesNo(false)
                game.resolveStack()

                // Library should be unchanged
                game.librarySize(1) shouldBe libraryBefore
                game.isOnBattlefield("Goblin Goon") shouldBe false
            }

            test("triggers when Skirk Drill Sergeant itself dies") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Skirk Drill Sergeant") // 2/1
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Goblin Goon")
                    .withCardInHand(2, "Shock") // 2 damage kills the 2/1
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sergeantId = game.findPermanent("Skirk Drill Sergeant")!!
                game.castSpell(2, "Shock", sergeantId)
                game.resolveStack() // Shock resolves, Drill Sergeant dies

                // Self-death trigger fires — "you may pay {2}{R}"
                game.hasPendingDecision() shouldBe true
                game.answerYesNo(true)
                game.submitManaSourcesAutoPay()
                game.resolveStack()

                // Goblin Goon should be on the battlefield
                game.isOnBattlefield("Goblin Goon") shouldBe true
                game.isInGraveyard(1, "Skirk Drill Sergeant") shouldBe true
            }
        }
    }
}
