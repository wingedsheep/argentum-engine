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
 * Scenario test for Improvised Armor.
 *
 * Card reference:
 * - Improvised Armor (3W): Enchantment — Aura
 *   "Enchant creature"
 *   "Enchanted creature gets +2/+5."
 *   "Cycling {3}"
 */
class ImprovisedArmorScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Improvised Armor aura enchantment") {
            test("casting Improvised Armor on a creature attaches it and gives +2/+5") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Improvised Armor")
                    .withCardOnBattlefield(1, "Devoted Hero") // 1/2 creature
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val heroId = game.findPermanent("Devoted Hero")!!

                // Cast Improvised Armor targeting Devoted Hero
                val castResult = game.castSpell(1, "Improvised Armor", heroId)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Improvised Armor should be on the battlefield
                withClue("Improvised Armor should be on the battlefield") {
                    game.isOnBattlefield("Improvised Armor") shouldBe true
                }

                // Improvised Armor should be attached to the creature
                val armorId = game.findPermanent("Improvised Armor")!!
                val armorEntity = game.state.getEntity(armorId)!!
                val attachedTo = armorEntity.get<AttachedToComponent>()
                withClue("Improvised Armor should be attached to Devoted Hero") {
                    attachedTo shouldNotBe null
                    attachedTo!!.targetId shouldBe heroId
                }

                // Creature should have boosted stats in projected state
                val projected = stateProjector.project(game.state)
                withClue("Devoted Hero power should be 3 (1 base + 2 from Improvised Armor), got ${projected.getPower(heroId)}") {
                    projected.getPower(heroId) shouldBe 3
                }
                withClue("Devoted Hero toughness should be 7 (2 base + 5 from Improvised Armor), got ${projected.getToughness(heroId)}") {
                    projected.getToughness(heroId) shouldBe 7
                }
            }
        }
    }
}
