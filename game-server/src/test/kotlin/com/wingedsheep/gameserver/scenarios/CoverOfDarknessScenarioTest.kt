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
 * Scenario tests for Cover of Darkness.
 *
 * Card reference:
 * - Cover of Darkness ({1}{B}): Enchantment
 *   "As Cover of Darkness enters the battlefield, choose a creature type.
 *    Creatures of the chosen type have fear."
 */
class CoverOfDarknessScenarioTest : ScenarioTestBase() {

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
        context("Cover of Darkness - choose creature type, creatures gain fear") {

            test("creatures of the chosen type have fear") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Cover of Darkness")
                    .withCardOnBattlefield(1, "Severed Legion") // Zombie, 2/2
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpell(1, "Cover of Darkness")
                withClue("Cover of Darkness should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Should have pending creature type decision") {
                    game.hasPendingDecision() shouldBe true
                }

                game.chooseCreatureType("Zombie")

                withClue("Cover of Darkness should be on battlefield") {
                    game.isOnBattlefield("Cover of Darkness") shouldBe true
                }

                val severedLegion = game.findPermanent("Severed Legion")!!
                val projected = stateProjector.project(game.state)

                withClue("Severed Legion (Zombie) should have fear") {
                    projected.hasKeyword(severedLegion, Keyword.FEAR) shouldBe true
                }
            }

            test("creatures of a different type do not gain fear") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Cover of Darkness")
                    .withCardOnBattlefield(1, "Elvish Warrior") // Elf Warrior, 2/3
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Cover of Darkness")
                game.resolveStack()
                game.chooseCreatureType("Zombie")

                val elfWarrior = game.findPermanent("Elvish Warrior")!!
                val projected = stateProjector.project(game.state)

                withClue("Elvish Warrior (Elf) should not have fear when Zombie is chosen") {
                    projected.hasKeyword(elfWarrior, Keyword.FEAR) shouldBe false
                }
            }

            test("affects both players' creatures of the chosen type") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Cover of Darkness")
                    .withCardOnBattlefield(1, "Severed Legion")    // Zombie, 2/2
                    .withCardOnBattlefield(2, "Gluttonous Zombie") // Zombie, 3/3
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Cover of Darkness")
                game.resolveStack()
                game.chooseCreatureType("Zombie")

                val projected = stateProjector.project(game.state)

                val severedLegion = game.findPermanent("Severed Legion")!!
                withClue("Player's Severed Legion (Zombie) should have fear") {
                    projected.hasKeyword(severedLegion, Keyword.FEAR) shouldBe true
                }

                val gluttonousZombie = game.findPermanent("Gluttonous Zombie")!!
                withClue("Opponent's Gluttonous Zombie (Zombie) should have fear") {
                    projected.hasKeyword(gluttonousZombie, Keyword.FEAR) shouldBe true
                }
            }

            test("stacks with Shared Triumph when same creature type is chosen") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Shared Triumph")
                    .withCardInHand(1, "Cover of Darkness")
                    .withCardOnBattlefield(1, "Severed Legion") // Zombie, 2/2
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Shared Triumph first
                game.castSpell(1, "Shared Triumph")
                game.resolveStack()
                game.chooseCreatureType("Zombie")

                // Cast Cover of Darkness
                game.castSpell(1, "Cover of Darkness")
                game.resolveStack()
                game.chooseCreatureType("Zombie")

                val severedLegion = game.findPermanent("Severed Legion")!!
                val projected = stateProjector.project(game.state)

                withClue("Severed Legion should have fear from Cover of Darkness") {
                    projected.hasKeyword(severedLegion, Keyword.FEAR) shouldBe true
                }

                withClue("Severed Legion (2/2 Zombie) should be 3/3 from Shared Triumph") {
                    projected.getPower(severedLegion) shouldBe 3
                    projected.getToughness(severedLegion) shouldBe 3
                }
            }
        }
    }
}
