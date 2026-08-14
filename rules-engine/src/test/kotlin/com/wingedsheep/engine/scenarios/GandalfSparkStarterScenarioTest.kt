package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Gandalf, Spark Starter (HOB) — {4}{R}{R} Legendary Creature — Avatar Wizard 4/3.
 *
 * "Reach
 *  When Gandalf enters, he deals 3 damage divided as you choose among one, two, or three targets."
 *
 * The division is announced with the targets as the trigger goes on the stack. "Any target" must
 * reach players as well as creatures, and every chosen target has to receive at least 1 damage.
 */
class GandalfSparkStarterScenarioTest : ScenarioTestBase() {

    init {
        context("Gandalf, Spark Starter") {

            test("it is a 4/3 with reach") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Gandalf, Spark Starter")
                    .build()

                val gandalf = game.findPermanent("Gandalf, Spark Starter")!!
                game.state.projectedState.getPower(gandalf) shouldBe 4
                game.state.projectedState.getToughness(gandalf) shouldBe 3
                game.state.projectedState.hasKeyword(gandalf, Keyword.REACH) shouldBe true
            }

            test("the ETB splits 3 damage across two chosen creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gandalf, Spark Starter")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val courser = game.findPermanent("Centaur Courser")!!

                game.castSpell(1, "Gandalf, Spark Starter").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the ETB trigger asks for its targets") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
                }
                game.selectTargets(listOf(bears, courser)).error shouldBe null

                val distribute = game.getPendingDecision()
                withClue("then the 3 damage is divided among them") {
                    (distribute is DistributeDecision) shouldBe true
                    (distribute as DistributeDecision).totalAmount shouldBe 3
                }
                game.submitDistribution(mapOf(bears to 2, courser to 1)).error shouldBe null
                game.resolveStack()

                withClue("2 damage is lethal to a 2/2") {
                    game.findPermanent("Grizzly Bears") shouldBe null
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe true
                }
                withClue("1 damage does not kill a 3/3") {
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                }
            }

            test("all 3 damage can go to a single target, including a player") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Gandalf, Spark Starter")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Gandalf, Spark Starter").error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                (game.getPendingDecision() is ChooseTargetsDecision) shouldBe true
                game.selectTargets(listOf(game.player2Id)).error shouldBe null

                val distribute = game.getPendingDecision()
                if (distribute is DistributeDecision) {
                    game.submitDistribution(mapOf(game.player2Id to 3)).error shouldBe null
                }
                game.resolveStack()

                withClue("'any target' includes a player") {
                    game.getLifeTotal(2) shouldBe 17
                }
            }
        }
    }
}
