package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Champion of the Flame.
 *
 * Card reference:
 * - Champion of the Flame ({1}{R}): Creature — Human Warrior, 1/1
 *   "Trample"
 *   "Champion of the Flame gets +2/+2 for each Aura and Equipment attached to it."
 */
class ChampionOfTheFlameScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Champion of the Flame attachment bonus") {

            test("gets +2/+2 when an Aura is attached") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Champion of the Flame")
                    .withCardInHand(1, "Demonic Vigor") // {B} Aura: +1/+1
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val championId = game.findPermanent("Champion of the Flame")!!

                // Base stats: 1/1
                val projectedBefore = stateProjector.project(game.state)
                withClue("Champion should be 1/1 with no attachments") {
                    projectedBefore.getPower(championId) shouldBe 1
                    projectedBefore.getToughness(championId) shouldBe 1
                }

                // Cast Demonic Vigor targeting Champion
                game.castSpell(1, "Demonic Vigor", championId)
                game.resolveStack()

                // Should be 1 + 2 (attachment bonus) + 1 (Demonic Vigor's own +1/+1) = 4/4
                val projectedAfter = stateProjector.project(game.state)
                withClue("Champion should be 4/4 (1 base + 2 from attachment count + 1 from Demonic Vigor)") {
                    projectedAfter.getPower(championId) shouldBe 4
                    projectedAfter.getToughness(championId) shouldBe 4
                }
            }

            test("gets +4/+4 with two attachments") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Champion of the Flame")
                    .withCardInHand(1, "Demonic Vigor")
                    .withCardInHand(1, "Frenzied Rage") // {1}{R} Aura: +2/+1, menace
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val championId = game.findPermanent("Champion of the Flame")!!

                // Cast first Aura
                game.castSpell(1, "Demonic Vigor", championId)
                game.resolveStack()

                // Cast second Aura
                game.castSpell(1, "Frenzied Rage", championId)
                game.resolveStack()

                // Should be 1 + 4 (2 attachments × +2/+2) + 1 (Demonic Vigor) + 2 (Frenzied Rage power) = 8
                // Toughness: 1 + 4 + 1 + 1 (Frenzied Rage toughness) = 7
                val projected = stateProjector.project(game.state)
                withClue("Champion power should be 8 (1 base + 4 from 2 attachments + 1 DV + 2 FR)") {
                    projected.getPower(championId) shouldBe 8
                }
                withClue("Champion toughness should be 7 (1 base + 4 from 2 attachments + 1 DV + 1 FR)") {
                    projected.getToughness(championId) shouldBe 7
                }
            }

            test("loses bonus when aura is removed") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Champion of the Flame")
                    .withCardInHand(1, "Demonic Vigor")
                    .withCardInHand(2, "Invoke the Divine") // {2}{W} destroy artifact or enchantment
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(2, "Plains", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val championId = game.findPermanent("Champion of the Flame")!!

                // Cast Demonic Vigor
                game.castSpell(1, "Demonic Vigor", championId)
                game.resolveStack()

                // Verify it's buffed
                val projectedWithAura = stateProjector.project(game.state)
                withClue("Champion should be 4/4 with one aura") {
                    projectedWithAura.getPower(championId) shouldBe 4
                    projectedWithAura.getToughness(championId) shouldBe 4
                }

                // Pass to opponent
                game.passPriority()

                // Opponent destroys the aura
                val auraId = game.findPermanent("Demonic Vigor")!!
                game.castSpell(2, "Invoke the Divine", auraId)
                game.resolveStack()

                // Champion should be back to 1/1
                val projectedAfter = stateProjector.project(game.state)
                withClue("Champion should be 1/1 after aura removed") {
                    projectedAfter.getPower(championId) shouldBe 1
                    projectedAfter.getToughness(championId) shouldBe 1
                }
            }
        }
    }
}
