package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Tests for Mardu Blazebringer's sacrifice-at-end-of-combat ability.
 *
 * Mardu Blazebringer: {2}{R}
 * Creature — Ogre Warrior 4/4
 * When Mardu Blazebringer attacks or blocks, sacrifice it at end of combat.
 */
class MarduBlazebringerScenarioTest : ScenarioTestBase() {

    init {
        context("Mardu Blazebringer attacks") {

            test("is sacrificed at end of combat after attacking") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Mardu Blazebringer", summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Move to declare attackers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Mardu Blazebringer" to 2))

                // Resolve the attack trigger (sacrifice at end of combat marker)
                game.resolveStack()

                // Pass through blockers and combat damage
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Mardu Blazebringer should be in graveyard
                game.isInGraveyard(1, "Mardu Blazebringer") shouldBe true
                game.isOnBattlefield("Mardu Blazebringer") shouldBe false

                // Defender should have taken 4 damage
                game.getLifeTotal(2) shouldBe 16
            }
        }

        context("Mardu Blazebringer blocks") {

            test("is sacrificed at end of combat after blocking") {
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(1, "Mardu Blazebringer", summoningSickness = false)
                    .withCardOnBattlefield(2, "Alpine Grizzly", summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Player 2 attacks with Alpine Grizzly (4/2)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Alpine Grizzly" to 1))

                // Advance to declare blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

                // Player 1 blocks with Mardu Blazebringer
                game.declareBlockers(mapOf("Mardu Blazebringer" to listOf("Alpine Grizzly")))

                // Resolve the block trigger (sacrifice at end of combat marker)
                game.resolveStack()

                // Pass through combat damage and end of combat
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Mardu Blazebringer should be in graveyard (sacrificed or died from combat damage)
                game.isInGraveyard(1, "Mardu Blazebringer") shouldBe true
                game.isOnBattlefield("Mardu Blazebringer") shouldBe false

                // Alpine Grizzly (4/2) should also be dead from combat damage (took 4 from Blazebringer)
                game.isInGraveyard(2, "Alpine Grizzly") shouldBe true
            }
        }
    }
}
