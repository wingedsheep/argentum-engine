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
 * Scenario tests for Tribal Unity.
 *
 * Card reference:
 * - Tribal Unity ({X}{2}{G}): Instant
 *   "Creatures of the creature type of your choice get +X/+X until end of turn."
 */
class TribalUnityScenarioTest : ScenarioTestBase() {

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
        context("Tribal Unity") {

            test("gives +X/+X to creatures of chosen type where X is the mana paid") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elvish Warrior")   // 2/3 Elf Warrior
                    .withCardOnBattlefield(1, "Wirewood Elf")     // 1/2 Elf Druid
                    .withCardOnBattlefield(1, "Glory Seeker")     // 2/2 Human Soldier
                    .withCardInHand(1, "Tribal Unity")
                    .withLandsOnBattlefield(1, "Forest", 5) // {2}{G} + X=2 → 5 mana total
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elvishWarriorId = game.findPermanent("Elvish Warrior")!!
                val wirewoodElfId = game.findPermanent("Wirewood Elf")!!
                val glorySeekerID = game.findPermanent("Glory Seeker")!!

                // Cast Tribal Unity with X=2 (total cost: {2}{2}{G} = 5 mana)
                game.castXSpell(1, "Tribal Unity", xValue = 2)
                game.chooseCreatureType("Elf")
                game.resolveStack()

                // All Elves should get +2/+2
                val clientState = game.getClientState(1)

                val elvishWarriorInfo = clientState.cards[elvishWarriorId]
                withClue("Elvish Warrior (2/3 Elf) should be 4/5 after +2/+2") {
                    elvishWarriorInfo shouldNotBe null
                    elvishWarriorInfo!!.power shouldBe 4
                    elvishWarriorInfo.toughness shouldBe 5
                }

                val wirewoodElfInfo = clientState.cards[wirewoodElfId]
                withClue("Wirewood Elf (1/2 Elf) should be 3/4 after +2/+2") {
                    wirewoodElfInfo shouldNotBe null
                    wirewoodElfInfo!!.power shouldBe 3
                    wirewoodElfInfo.toughness shouldBe 4
                }

                // Non-Elf should NOT be affected
                val glorySeekerInfo = clientState.cards[glorySeekerID]
                withClue("Glory Seeker (2/2 Human) should remain 2/2") {
                    glorySeekerInfo shouldNotBe null
                    glorySeekerInfo!!.power shouldBe 2
                    glorySeekerInfo.toughness shouldBe 2
                }
            }

            test("X=0 gives +0/+0 (no change)") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elvish Warrior")   // 2/3 Elf Warrior
                    .withCardInHand(1, "Tribal Unity")
                    .withLandsOnBattlefield(1, "Forest", 3) // {2}{G} + X=0 → 3 mana total
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elvishWarriorId = game.findPermanent("Elvish Warrior")!!

                game.castXSpell(1, "Tribal Unity", xValue = 0)
                game.chooseCreatureType("Elf")
                game.resolveStack()

                val clientState = game.getClientState(1)
                val elvishWarriorInfo = clientState.cards[elvishWarriorId]
                withClue("Elvish Warrior (2/3 Elf) should remain 2/3 with X=0") {
                    elvishWarriorInfo shouldNotBe null
                    elvishWarriorInfo!!.power shouldBe 2
                    elvishWarriorInfo.toughness shouldBe 3
                }
            }

            test("also affects opponent's creatures of the chosen type") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elvish Warrior")   // 2/3 Elf Warrior
                    .withCardOnBattlefield(2, "Wellwisher")       // 1/1 Elf
                    .withCardInHand(1, "Tribal Unity")
                    .withLandsOnBattlefield(1, "Forest", 6) // {2}{G} + X=3 → 6 mana total
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elvishWarriorId = game.findPermanent("Elvish Warrior")!!
                val wellwisherId = game.findPermanent("Wellwisher")!!

                game.castXSpell(1, "Tribal Unity", xValue = 3)
                game.chooseCreatureType("Elf")
                game.resolveStack()

                val clientState = game.getClientState(1)

                val elvishWarriorInfo = clientState.cards[elvishWarriorId]
                withClue("Elvish Warrior (2/3 Elf) should be 5/6 after +3/+3") {
                    elvishWarriorInfo shouldNotBe null
                    elvishWarriorInfo!!.power shouldBe 5
                    elvishWarriorInfo.toughness shouldBe 6
                }

                val wellwisherInfo = clientState.cards[wellwisherId]
                withClue("Opponent's Wellwisher (1/1 Elf) should be 4/4 after +3/+3") {
                    wellwisherInfo shouldNotBe null
                    wellwisherInfo!!.power shouldBe 4
                    wellwisherInfo.toughness shouldBe 4
                }
            }

            test("buff expires at end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elvish Warrior")   // 2/3 Elf Warrior
                    .withCardInHand(1, "Tribal Unity")
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elvishWarriorId = game.findPermanent("Elvish Warrior")!!

                game.castXSpell(1, "Tribal Unity", xValue = 2)
                game.chooseCreatureType("Elf")
                game.resolveStack()

                // Verify buff is active
                val duringTurn = game.getClientState(1).cards[elvishWarriorId]
                withClue("Elvish Warrior should be 4/5 during P1's turn") {
                    duringTurn!!.power shouldBe 4
                    duringTurn.toughness shouldBe 5
                }

                // Advance to P2's turn
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                withClue("Active player should now be P2") {
                    game.state.activePlayerId shouldBe game.player2Id
                }

                // Verify buff has expired
                val afterTurn = game.getClientState(1).cards[elvishWarriorId]
                withClue("Elvish Warrior should be back to 2/3 after turn ends") {
                    afterTurn!!.power shouldBe 2
                    afterTurn.toughness shouldBe 3
                }
            }
        }
    }
}
