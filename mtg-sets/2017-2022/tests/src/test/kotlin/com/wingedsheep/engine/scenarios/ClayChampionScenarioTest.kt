package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Clay Champion (BRO #230).
 *
 *   This creature enters with three +1/+1 counters on it for each {G}{G} spent to cast it.
 *   When this creature enters, choose up to two other target creatures you control. For each
 *   {W}{W} spent to cast this creature, put a +1/+1 counter on each of them.
 *
 * The {G}{G} count is read as the creature enters (a replacement), the {W}{W} count when the
 * enters trigger resolves — both off the payment recorded on the permanent. Each scenario taps
 * exactly as many lands as the cost needs, so the colors spent are the lands on the board.
 */
class ClayChampionScenarioTest : ScenarioTestBase() {

    private fun TestGame.castChampion(x: Int, targets: List<EntityId>) {
        castXSpell(1, "Clay Champion", xValue = x).error shouldBe null
        resolveStack()
        var guard = 0
        while (hasPendingDecision() && guard++ < 5) {
            when (val decision = getPendingDecision()!!) {
                is OrderObjectsDecision -> submitDecision(OrderedResponse(decision.id, decision.objects))
                else -> selectTargets(targets)
            }
            resolveStack()
        }
    }

    private fun TestGame.bears(): List<EntityId> =
        state.getBattlefield().filter { state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Grizzly Bears" }

    init {
        context("Clay Champion") {

            test("{X} paid in green counts: four {G} is two pairs, six counters; {W}{W} gives each target one") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Clay Champion")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.bears()
                game.castChampion(x = 2, targets = bears)

                val champion = game.findPermanent("Clay Champion")!!
                val projected = game.state.projectedState
                withClue("2/2 plus six counters") { projected.getPower(champion) shouldBe 8 }
                bears.forEach { projected.getPower(it) shouldBe 3 }
            }

            test("odd leftovers round down: three {G} is one pair, one {W} gives nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Clay Champion")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.bears()
                game.castChampion(x = 0, targets = bears)

                val projected = game.state.projectedState
                projected.getPower(game.findPermanent("Clay Champion")!!) shouldBe 5
                bears.forEach { projected.getPower(it) shouldBe 2 }
            }

            test("no green spent: the champion enters as a 2/2, and four {W} gives each target two counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Clay Champion")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.bears()
                game.castChampion(x = 0, targets = bears)

                val projected = game.state.projectedState
                projected.getPower(game.findPermanent("Clay Champion")!!) shouldBe 2
                bears.forEach { projected.getPower(it) shouldBe 4 }
            }
        }
    }
}
