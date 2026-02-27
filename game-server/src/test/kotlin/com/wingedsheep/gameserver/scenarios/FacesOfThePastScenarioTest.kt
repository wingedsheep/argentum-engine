package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Faces of the Past.
 *
 * Card reference:
 * - Faces of the Past ({2}{U}): Enchantment
 *   "Whenever a creature dies, tap all untapped creatures that share a creature type
 *   with it or untap all tapped creatures that share a creature type with it."
 */
class FacesOfThePastScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseMode(modeIndex: Int) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(OptionChosenResponse(decision.id, modeIndex))
    }

    private fun isTapped(game: TestGame, cardName: String): Boolean {
        val entityId = game.findPermanent(cardName)!!
        return game.state.getEntity(entityId)?.has<TappedComponent>() == true
    }

    init {
        context("Faces of the Past triggered ability") {

            test("choosing tap mode taps all creatures sharing a type with the dying creature") {
                // Glory Seeker (2/2 Human Soldier) and Daru Cavalier (2/2 Human Soldier Knight)
                // share the Human and Soldier types. Kill Daru Cavalier, choose tap.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Faces of the Past")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardOnBattlefield(2, "Daru Cavalier")
                    .withCardInHand(1, "Volcanic Hammer")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val daruCavalierId = game.findPermanent("Daru Cavalier")!!

                // Cast Volcanic Hammer targeting Daru Cavalier (3 damage kills the 2/2)
                game.castSpell(1, "Volcanic Hammer", daruCavalierId)
                game.resolveStack()

                // Faces of the Past triggers — choose mode 0 (tap)
                game.chooseMode(0)

                // Glory Seeker shares Human/Soldier with Daru Cavalier, should be tapped
                withClue("Glory Seeker should be tapped (shares Soldier type with Daru Cavalier)") {
                    isTapped(game, "Glory Seeker") shouldBe true
                }
            }

            test("choosing untap mode untaps all creatures sharing a type with the dying creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Faces of the Past")
                    .withCardOnBattlefield(1, "Glory Seeker", tapped = true)
                    .withCardOnBattlefield(2, "Daru Cavalier")
                    .withCardInHand(1, "Volcanic Hammer")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val daruCavalierId = game.findPermanent("Daru Cavalier")!!

                // Confirm Glory Seeker starts tapped
                withClue("Glory Seeker should start tapped") {
                    isTapped(game, "Glory Seeker") shouldBe true
                }

                // Cast Volcanic Hammer targeting Daru Cavalier
                game.castSpell(1, "Volcanic Hammer", daruCavalierId)
                game.resolveStack()

                // Faces of the Past triggers — choose mode 1 (untap)
                game.chooseMode(1)

                // Glory Seeker shares Soldier type, should be untapped
                withClue("Glory Seeker should be untapped (shares Soldier type with Daru Cavalier)") {
                    isTapped(game, "Glory Seeker") shouldBe false
                }
            }

            test("creatures that don't share a type are unaffected") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Faces of the Past")
                    .withCardOnBattlefield(1, "Elvish Warrior") // 2/3 Elf Warrior
                    .withCardOnBattlefield(2, "Daru Cavalier") // 2/2 Human Soldier Knight
                    .withCardInHand(1, "Volcanic Hammer")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val daruCavalierId = game.findPermanent("Daru Cavalier")!!
                game.castSpell(1, "Volcanic Hammer", daruCavalierId)
                game.resolveStack()

                // Faces of the Past triggers — choose mode 0 (tap)
                game.chooseMode(0)

                // Elvish Warrior is an Elf Warrior, no shared type with Human Soldier Knight
                withClue("Elvish Warrior should NOT be tapped (no shared type with Daru Cavalier)") {
                    isTapped(game, "Elvish Warrior") shouldBe false
                }
            }
        }
    }
}
