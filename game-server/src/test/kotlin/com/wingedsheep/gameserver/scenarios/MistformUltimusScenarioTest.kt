package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Mistform Ultimus (Changeling keyword support).
 *
 * Card reference:
 * - Mistform Ultimus ({3}{U}): 3/3 Legendary Creature — Illusion
 *   "Mistform Ultimus is every creature type (even if this card isn't on the battlefield)."
 *
 * Changeling means the creature has all creature types in all zones.
 * Tests verify that Changeling creatures:
 * 1. Have all creature types in projected state on the battlefield
 * 2. Benefit from tribal lord effects (e.g., Goblin lord, Soldier lord)
 */
class MistformUltimusScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Mistform Ultimus has all creature types") {

            test("has all creature types on the battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mistform Ultimus")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ultimus = game.findPermanent("Mistform Ultimus")!!
                val projected = stateProjector.project(game.state)
                val subtypes = projected.getSubtypes(ultimus)

                withClue("Should have Goblin subtype") {
                    subtypes.any { it.equals("Goblin", ignoreCase = true) } shouldBe true
                }
                withClue("Should have Elf subtype") {
                    subtypes.any { it.equals("Elf", ignoreCase = true) } shouldBe true
                }
                withClue("Should have Wizard subtype") {
                    subtypes.any { it.equals("Wizard", ignoreCase = true) } shouldBe true
                }
                withClue("Should have Illusion subtype (printed type)") {
                    subtypes.any { it.equals("Illusion", ignoreCase = true) } shouldBe true
                }
                withClue("Should have all creature types") {
                    for (creatureType in Subtype.ALL_CREATURE_TYPES) {
                        subtypes.any { it.equals(creatureType, ignoreCase = true) } shouldBe true
                    }
                }
            }

            test("benefits from tribal lord effects") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Mistform Ultimus")
                    .withCardOnBattlefield(1, "Timberwatch Elf") // Elf lord: tap to give +X/+X
                    .withCardOnBattlefield(1, "Aven Brigadier") // Other Birds +1/+1, other Soldiers +1/+1
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val ultimus = game.findPermanent("Mistform Ultimus")!!
                val projected = stateProjector.project(game.state)

                withClue("Mistform Ultimus (3/3) should get +1/+1 from Bird lord and +1/+1 from Soldier lord = 5/5") {
                    projected.getPower(ultimus) shouldBe 5
                    projected.getToughness(ultimus) shouldBe 5
                }
            }
        }
    }
}
