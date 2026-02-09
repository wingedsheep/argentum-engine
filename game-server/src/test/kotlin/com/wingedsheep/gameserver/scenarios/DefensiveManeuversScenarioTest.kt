package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Defensive Maneuvers.
 *
 * Card reference:
 * - Defensive Maneuvers ({3}{W}): Instant
 *   "Creatures of the creature type of your choice get +0/+4 until end of turn."
 */
class DefensiveManeuversScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseCreatureType(typeName: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val options = (decision as ChooseOptionDecision).options
        val index = options.indexOf(typeName)
        withClue("Creature type '$typeName' should be in options $options") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        context("Defensive Maneuvers") {

            test("gives +0/+4 to all creatures of the chosen type") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elvish Warrior")   // 2/3 Elf Warrior
                    .withCardOnBattlefield(1, "Wirewood Elf")     // 1/2 Elf Druid
                    .withCardOnBattlefield(2, "Wellwisher")       // 0/1 Elf Cleric
                    .withCardOnBattlefield(1, "Glory Seeker")     // 2/2 Human Soldier
                    .withCardInHand(1, "Defensive Maneuvers")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elvishWarriorId = game.findPermanent("Elvish Warrior")!!
                val wirewoodElfId = game.findPermanent("Wirewood Elf")!!
                val wellwisherId = game.findPermanent("Wellwisher")!!
                val glorySeekerID = game.findPermanent("Glory Seeker")!!

                // Cast Defensive Maneuvers (no targets required)
                val result = game.castSpell(1, "Defensive Maneuvers")
                withClue("Cast should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Should be asked to choose a creature type
                game.chooseCreatureType("Elf")

                // All Elves should get +0/+4
                val clientState = game.getClientState(1)

                val elvishWarriorInfo = clientState.cards[elvishWarriorId]
                withClue("Elvish Warrior (2/3 Elf) should be 2/7 after +0/+4") {
                    elvishWarriorInfo shouldNotBe null
                    elvishWarriorInfo!!.power shouldBe 2
                    elvishWarriorInfo.toughness shouldBe 7
                }

                val wirewoodElfInfo = clientState.cards[wirewoodElfId]
                withClue("Wirewood Elf (1/2 Elf) should be 1/6 after +0/+4") {
                    wirewoodElfInfo shouldNotBe null
                    wirewoodElfInfo!!.power shouldBe 1
                    wirewoodElfInfo.toughness shouldBe 6
                }

                // Opponent's Elf also gets the bonus
                val wellwisherInfo = clientState.cards[wellwisherId]
                withClue("Wellwisher (1/1 Elf) should be 1/5 after +0/+4") {
                    wellwisherInfo shouldNotBe null
                    wellwisherInfo!!.power shouldBe 1
                    wellwisherInfo.toughness shouldBe 5
                }

                // Non-Elf should NOT be affected
                val glorySeekerInfo = clientState.cards[glorySeekerID]
                withClue("Glory Seeker (2/2 Human) should remain 2/2") {
                    glorySeekerInfo shouldNotBe null
                    glorySeekerInfo!!.power shouldBe 2
                    glorySeekerInfo.toughness shouldBe 2
                }
            }

            test("does nothing if no creatures of the chosen type exist") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Glory Seeker")     // 2/2 Human Soldier
                    .withCardInHand(1, "Defensive Maneuvers")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeekerID = game.findPermanent("Glory Seeker")!!

                game.castSpell(1, "Defensive Maneuvers")
                game.resolveStack()

                // Choose a type that doesn't exist on the battlefield
                game.chooseCreatureType("Dragon")

                // Glory Seeker should be unaffected
                val clientState = game.getClientState(1)
                val glorySeekerInfo = clientState.cards[glorySeekerID]
                withClue("Glory Seeker should remain 2/2") {
                    glorySeekerInfo shouldNotBe null
                    glorySeekerInfo!!.power shouldBe 2
                    glorySeekerInfo.toughness shouldBe 2
                }
            }
        }
    }
}
