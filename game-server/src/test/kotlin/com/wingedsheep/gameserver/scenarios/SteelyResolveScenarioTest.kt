package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Steely Resolve.
 *
 * Card reference:
 * - Steely Resolve ({1}{G}): Enchantment
 *   "As Steely Resolve enters the battlefield, choose a creature type.
 *    Creatures of the chosen type have shroud."
 */
class SteelyResolveScenarioTest : ScenarioTestBase() {

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
        context("Steely Resolve - choose creature type, creatures gain shroud") {

            test("creatures of the chosen type have shroud") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Steely Resolve")
                    .withCardOnBattlefield(1, "Elvish Warrior") // Elf Warrior, 2/3
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Steely Resolve")
                withClue("Steely Resolve should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Should have pending creature type decision") {
                    game.hasPendingDecision() shouldBe true
                }

                game.chooseCreatureType("Elf")

                withClue("Steely Resolve should be on battlefield") {
                    game.isOnBattlefield("Steely Resolve") shouldBe true
                }

                val elvishWarrior = game.findPermanent("Elvish Warrior")!!
                val projected = stateProjector.project(game.state)

                withClue("Elvish Warrior (Elf) should have shroud") {
                    projected.hasKeyword(elvishWarrior, Keyword.SHROUD) shouldBe true
                }
            }

            test("creatures of a different type do not gain shroud") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Steely Resolve")
                    .withCardOnBattlefield(1, "Severed Legion") // Zombie, 2/2
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Steely Resolve")
                game.resolveStack()
                game.chooseCreatureType("Elf")

                val severedLegion = game.findPermanent("Severed Legion")!!
                val projected = stateProjector.project(game.state)

                withClue("Severed Legion (Zombie) should not have shroud when Elf is chosen") {
                    projected.hasKeyword(severedLegion, Keyword.SHROUD) shouldBe false
                }
            }

            test("affects both players' creatures of the chosen type") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Steely Resolve")
                    .withCardOnBattlefield(1, "Elvish Warrior")  // Elf Warrior, 2/3
                    .withCardOnBattlefield(2, "Wellwisher")      // Elf, 1/1
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Steely Resolve")
                game.resolveStack()
                game.chooseCreatureType("Elf")

                val projected = stateProjector.project(game.state)

                val elvishWarrior = game.findPermanent("Elvish Warrior")!!
                withClue("Player's Elvish Warrior (Elf) should have shroud") {
                    projected.hasKeyword(elvishWarrior, Keyword.SHROUD) shouldBe true
                }

                val wellwisher = game.findPermanent("Wellwisher")!!
                withClue("Opponent's Wellwisher (Elf) should have shroud") {
                    projected.hasKeyword(wellwisher, Keyword.SHROUD) shouldBe true
                }
            }
        }
    }
}
