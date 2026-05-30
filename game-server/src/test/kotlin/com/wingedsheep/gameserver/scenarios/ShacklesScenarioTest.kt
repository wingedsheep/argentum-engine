package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Shackles (EXO canonical, reprinted in INV).
 *
 * Card reference:
 * - Shackles ({2}{W}): Enchantment — Aura
 *   "Enchant creature"
 *   "Enchanted creature doesn't untap during its controller's untap step."
 *   "{W}: Return this Aura to its owner's hand."
 *
 * Verifies the DOESNT_UNTAP ability flag granted to the enchanted creature both shows
 * up in projected state and is respected by the untap step.
 */
class ShacklesScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Shackles keeps the enchanted creature tapped") {
            test("enchanted creature gains DOESNT_UNTAP in projected state") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Shackles")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                val castResult = game.castSpell(1, "Shackles", bears)
                withClue("Cast should succeed") { castResult.error shouldBe null }
                game.resolveStack()

                withClue("Shackles should be on the battlefield") {
                    game.isOnBattlefield("Shackles") shouldBe true
                }

                val projected = stateProjector.project(game.state)
                withClue("Enchanted creature should have DOESNT_UNTAP") {
                    projected.hasKeyword(bears, AbilityFlag.DOESNT_UNTAP) shouldBe true
                }
            }

            test("enchanted creature stays tapped through its controller's untap step") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Shackles")
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Shackles", bears)
                game.resolveStack()

                withClue("Creature should be tapped before opponent's untap step") {
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                }

                // Advance into the opponent's turn, through their untap step.
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                withClue("Enchanted creature must NOT untap during its controller's untap step") {
                    game.state.getEntity(bears)!!.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
