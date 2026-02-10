package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Shared Triumph.
 *
 * Card reference:
 * - Shared Triumph ({1}{W}): Enchantment
 *   "As Shared Triumph enters the battlefield, choose a creature type.
 *    Creatures of the chosen type get +1/+1."
 */
class SharedTriumphScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    private fun TestGame.chooseCreatureType(typeName: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val options = (decision as ChooseOptionDecision).options
        val index = options.indexOf(typeName)
        withClue("Creature type '$typeName' should be in options") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        context("Shared Triumph - choose creature type, creatures get +1/+1") {

            test("creatures of the chosen type get +1/+1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Shared Triumph")
                    .withCardOnBattlefield(1, "Elvish Warrior") // Elf Warrior, 2/3
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Shared Triumph
                val castResult = game.castSpell(1, "Shared Triumph")
                withClue("Shared Triumph should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve - should pause for creature type choice
                game.resolveStack()

                withClue("Should have pending creature type decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Choose "Elf"
                game.chooseCreatureType("Elf")

                // Shared Triumph should be on the battlefield
                withClue("Shared Triumph should be on battlefield") {
                    game.isOnBattlefield("Shared Triumph") shouldBe true
                }

                // Elvish Warrior (2/3 Elf) should be 3/4
                val elfWarrior = game.findPermanent("Elvish Warrior")!!
                val projected = stateProjector.project(game.state)

                withClue("Elvish Warrior (Elf Warrior 2/3) should be 3/4 with Elf lord bonus") {
                    projected.getPower(elfWarrior) shouldBe 3
                    projected.getToughness(elfWarrior) shouldBe 4
                }
            }

            test("creatures of a different type are not affected") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Shared Triumph")
                    .withCardOnBattlefield(1, "Goblin Sky Raider") // Goblin Warrior, 1/2
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Shared Triumph")
                game.resolveStack()
                game.chooseCreatureType("Elf")

                val goblin = game.findPermanent("Goblin Sky Raider")!!
                val projected = stateProjector.project(game.state)

                withClue("Goblin Sky Raider (Goblin 1/2) should remain 1/2 - not an Elf") {
                    projected.getPower(goblin) shouldBe 1
                    projected.getToughness(goblin) shouldBe 2
                }
            }

            test("affects both players' creatures of the chosen type") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Shared Triumph")
                    .withCardOnBattlefield(1, "Elvish Warrior") // Elf Warrior, 2/3
                    .withCardOnBattlefield(2, "Wirewood Elf")   // Elf Druid, 1/2
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Shared Triumph")
                game.resolveStack()
                game.chooseCreatureType("Elf")

                val projected = stateProjector.project(game.state)

                val elfWarrior = game.findPermanent("Elvish Warrior")!!
                withClue("Player's Elvish Warrior (2/3) should be 3/4") {
                    projected.getPower(elfWarrior) shouldBe 3
                    projected.getToughness(elfWarrior) shouldBe 4
                }

                val wirewoodElf = game.findPermanent("Wirewood Elf")!!
                withClue("Opponent's Wirewood Elf (1/2) should be 2/3") {
                    projected.getPower(wirewoodElf) shouldBe 2
                    projected.getToughness(wirewoodElf) shouldBe 3
                }
            }

            test("stacks with Aven Brigadier lord effects") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Shared Triumph")
                    .withCardOnBattlefield(1, "Aven Brigadier") // Bird Soldier 3/5, lord for Bird+Soldier
                    .withCardOnBattlefield(1, "Sage Aven")      // Bird Wizard 1/3
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Shared Triumph")
                game.resolveStack()
                game.chooseCreatureType("Bird")

                val projected = stateProjector.project(game.state)

                // Sage Aven: base 1/3, +1/+1 from Aven Brigadier (Bird lord), +1/+1 from Shared Triumph (Bird) = 3/5
                val sageAven = game.findPermanent("Sage Aven")!!
                withClue("Sage Aven (1/3 Bird) should be 3/5: +1/+1 from Brigadier + +1/+1 from Shared Triumph") {
                    projected.getPower(sageAven) shouldBe 3
                    projected.getToughness(sageAven) shouldBe 5
                }

                // Aven Brigadier: base 3/5, +1/+1 from Shared Triumph (Bird) only (Brigadier says "other")
                val brigadier = game.findPermanent("Aven Brigadier")!!
                withClue("Aven Brigadier (3/5 Bird Soldier) should be 4/6: +1/+1 from Shared Triumph") {
                    projected.getPower(brigadier) shouldBe 4
                    projected.getToughness(brigadier) shouldBe 6
                }
            }
        }
    }
}
