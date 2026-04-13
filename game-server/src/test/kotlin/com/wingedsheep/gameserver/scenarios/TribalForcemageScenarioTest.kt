package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Tribal Forcemage.
 *
 * Card reference:
 * - Tribal Forcemage ({1}{G}): Creature — Elf Wizard, 1/1
 *   Morph {1}{G}
 *   When this creature is turned face up, creatures of the creature type of your choice
 *   get +2/+2 and gain trample until end of turn.
 */
class TribalForcemageScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseCreatureType(typeName: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val options = decision.options
        val index = options.indexOf(typeName)
        withClue("Creature type '$typeName' should be in options $options") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        context("Tribal Forcemage") {

            test("turning face up gives chosen creature type +2/+2 and trample") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Tribal Forcemage")
                    .withCardOnBattlefield(1, "Elvish Warrior")   // 2/3 Elf Warrior
                    .withCardOnBattlefield(1, "Glory Seeker")     // 2/2 Human Soldier
                    .withLandsOnBattlefield(1, "Forest", 5)       // {3} cast face-down + {1}{G} morph
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elvishWarriorId = game.findPermanent("Elvish Warrior")!!
                val glorySeekerID = game.findPermanent("Glory Seeker")!!

                // Cast Tribal Forcemage face-down
                val forcemageId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Tribal Forcemage"
                }
                game.execute(CastSpell(game.player1Id, forcemageId, castFaceDown = true))
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                // Turn face up for {1}{G}
                val turnUpResult = game.execute(TurnFaceUp(game.player1Id, faceDownId))
                withClue("Turn face-up should succeed: ${turnUpResult.error}") {
                    turnUpResult.error shouldBe null
                }

                // Resolve the triggered ability — this pauses for creature type choice
                game.resolveStack()

                // Choose creature type "Elf"
                game.chooseCreatureType("Elf")

                val projected = StateProjector().project(game.state)

                // Elvish Warrior (Elf) should get +2/+2 and trample
                val clientState = game.getClientState(1)
                val elvishWarriorInfo = clientState.cards[elvishWarriorId]
                withClue("Elvish Warrior (2/3 Elf) should be 4/5 after +2/+2") {
                    elvishWarriorInfo shouldNotBe null
                    elvishWarriorInfo!!.power shouldBe 4
                    elvishWarriorInfo.toughness shouldBe 5
                }
                withClue("Elvish Warrior should have trample") {
                    projected.hasKeyword(elvishWarriorId, Keyword.TRAMPLE) shouldBe true
                }

                // Tribal Forcemage is an Elf, so it should also get +2/+2 and trample (now 3/3)
                val forcemageInfo = clientState.cards[faceDownId]
                withClue("Tribal Forcemage (1/1 Elf) should be 3/3 after +2/+2") {
                    forcemageInfo shouldNotBe null
                    forcemageInfo!!.power shouldBe 3
                    forcemageInfo.toughness shouldBe 3
                }
                withClue("Tribal Forcemage should have trample") {
                    projected.hasKeyword(faceDownId, Keyword.TRAMPLE) shouldBe true
                }

                // Glory Seeker (Human Soldier, not an Elf) should NOT be affected
                val glorySeekerInfo = clientState.cards[glorySeekerID]
                withClue("Glory Seeker (2/2 Human) should remain 2/2") {
                    glorySeekerInfo shouldNotBe null
                    glorySeekerInfo!!.power shouldBe 2
                    glorySeekerInfo.toughness shouldBe 2
                }
                withClue("Glory Seeker should NOT have trample") {
                    projected.hasKeyword(glorySeekerID, Keyword.TRAMPLE) shouldBe false
                }
            }

            test("buff and trample expire at end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Tribal Forcemage")
                    .withCardOnBattlefield(1, "Elvish Warrior")   // 2/3 Elf
                    .withLandsOnBattlefield(1, "Forest", 5)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elvishWarriorId = game.findPermanent("Elvish Warrior")!!

                // Cast face-down and turn face-up
                val forcemageId = game.state.getHand(game.player1Id).first { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Tribal Forcemage"
                }
                game.execute(CastSpell(game.player1Id, forcemageId, castFaceDown = true))
                game.resolveStack()

                val faceDownId = game.state.getBattlefield().find { entityId ->
                    game.state.getEntity(entityId)?.has<FaceDownComponent>() == true
                }!!

                game.execute(TurnFaceUp(game.player1Id, faceDownId))
                game.resolveStack()
                game.chooseCreatureType("Elf")

                // Verify buff is active during this turn
                val projectedDuring = StateProjector().project(game.state)
                withClue("Elvish Warrior should have trample during turn") {
                    projectedDuring.hasKeyword(elvishWarriorId, Keyword.TRAMPLE) shouldBe true
                }
                val duringTurn = game.getClientState(1).cards[elvishWarriorId]
                withClue("Elvish Warrior should be 4/5 during turn") {
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
                val projectedAfter = StateProjector().project(game.state)
                withClue("Elvish Warrior should NOT have trample after turn ends") {
                    projectedAfter.hasKeyword(elvishWarriorId, Keyword.TRAMPLE) shouldBe false
                }
                val afterTurn = game.getClientState(1).cards[elvishWarriorId]
                withClue("Elvish Warrior should be back to 2/3 after turn ends") {
                    afterTurn!!.power shouldBe 2
                    afterTurn.toughness shouldBe 3
                }
            }
        }
    }
}
