package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Clutch of Undeath.
 *
 * Card reference:
 * - Clutch of Undeath (3BB): Enchantment — Aura
 *   "Enchant creature"
 *   "Enchanted creature gets +3/+3 as long as it's a Zombie. Otherwise, it gets -3/-3."
 */
class ClutchOfUndeathScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Clutch of Undeath aura enchantment") {
            test("gives +3/+3 to a Zombie creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Clutch of Undeath")
                    .withCardOnBattlefield(1, "Twisted Abomination") // 5/3 Zombie
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val zombieId = game.findPermanent("Twisted Abomination")!!

                // Cast Clutch of Undeath targeting the Zombie
                val castResult = game.castSpell(1, "Clutch of Undeath", zombieId)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Clutch of Undeath should be on the battlefield attached to the Zombie
                withClue("Clutch of Undeath should be on the battlefield") {
                    game.isOnBattlefield("Clutch of Undeath") shouldBe true
                }

                val auraId = game.findPermanent("Clutch of Undeath")!!
                val auraEntity = game.state.getEntity(auraId)!!
                val attachedTo = auraEntity.get<AttachedToComponent>()
                withClue("Clutch of Undeath should be attached to Twisted Abomination") {
                    attachedTo shouldNotBe null
                    attachedTo!!.targetId shouldBe zombieId
                }

                // Zombie should get +3/+3 (5+3=8 power, 3+3=6 toughness)
                val projected = stateProjector.project(game.state)
                withClue("Twisted Abomination power should be 8 (5 base + 3 from Clutch)") {
                    projected.getPower(zombieId) shouldBe 8
                }
                withClue("Twisted Abomination toughness should be 6 (3 base + 3 from Clutch)") {
                    projected.getToughness(zombieId) shouldBe 6
                }
            }

            test("gives -3/-3 to a non-Zombie creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Clutch of Undeath")
                    .withCardOnBattlefield(1, "Elvish Aberration") // 4/5 Elf Mutant
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elfId = game.findPermanent("Elvish Aberration")!!

                // Cast Clutch of Undeath targeting the non-Zombie
                val castResult = game.castSpell(1, "Clutch of Undeath", elfId)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Clutch of Undeath should be on the battlefield
                withClue("Clutch of Undeath should be on the battlefield") {
                    game.isOnBattlefield("Clutch of Undeath") shouldBe true
                }

                // Non-Zombie should get -3/-3 (4-3=1 power, 5-3=2 toughness)
                val projected = stateProjector.project(game.state)
                withClue("Elvish Aberration power should be 1 (4 base - 3 from Clutch)") {
                    projected.getPower(elfId) shouldBe 1
                }
                withClue("Elvish Aberration toughness should be 2 (5 base - 3 from Clutch)") {
                    projected.getToughness(elfId) shouldBe 2
                }
            }
        }
    }
}
