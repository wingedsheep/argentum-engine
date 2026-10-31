package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Primal Empathy (IKO): the upkeep trigger draws when you control a creature tied for or holding
 * the greatest power on the battlefield, and otherwise puts a +1/+1 counter on a creature you
 * choose (not targeted) — the if/else is decided as the trigger resolves.
 */
class PrimalEmpathyScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Player 1 controls Primal Empathy; the game sits at the opponent's end step, one pass from our upkeep. */
    private fun builder() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Primal Empathy")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(2, "Forest")
        .withActivePlayer(2)
        .inPhase(Phase.ENDING, Step.END)

    init {
        context("Primal Empathy") {
            test("controlling the biggest creature draws a card and adds no counter") {
                val game = builder()
                    .withCardOnBattlefield(1, "Hill Giant") // 3/3
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2
                    .build()
                val handBefore = game.handSize(1)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("the trigger drew a card during upkeep") { game.handSize(1) shouldBe handBefore + 1 }
                game.plusOnes(game.findPermanent("Hill Giant")!!) shouldBe 0
            }

            test("a tie for greatest power still counts as controlling it") {
                val game = builder()
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .build()
                val handBefore = game.handSize(1)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                game.handSize(1) shouldBe handBefore + 1
            }

            test("otherwise a chosen creature you control gets a +1/+1 counter and no card is drawn") {
                val game = builder()
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                    .withCardOnBattlefield(1, "Llanowar Elves") // 1/1
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .build()
                val handBefore = game.handSize(1)

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("the counter's creature is chosen on resolution") { game.hasPendingDecision() shouldBe true }
                val elves = game.findPermanent("Llanowar Elves")!!
                game.selectTargets(listOf(elves)).error shouldBe null
                game.resolveStack()

                game.plusOnes(elves) shouldBe 1
                game.plusOnes(game.findPermanent("Grizzly Bears")!!) shouldBe 0
                game.handSize(1) shouldBe handBefore
            }
        }
    }
}
