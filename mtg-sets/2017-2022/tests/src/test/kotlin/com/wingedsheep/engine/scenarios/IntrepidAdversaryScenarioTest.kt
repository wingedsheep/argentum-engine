package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Intrepid Adversary (Innistrad: Midnight Hunt #25).
 *
 * When this creature enters, you may pay {1}{W} any number of times. When you pay this cost one
 * or more times, put that many valor counters on this creature.
 * Creatures you control get +1/+1 for each valor counter on this creature.
 */
class IntrepidAdversaryScenarioTest : ScenarioTestBase() {

    private fun TestGame.valor(): Int =
        state.getEntity(findPermanent("Intrepid Adversary")!!)?.get<CountersComponent>()
            ?.getCount(CounterType.VALOR) ?: 0

    /** Cast the Adversary off two Plains, leaving [spare] Plains untapped. */
    private fun castAdversary(spare: Int): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Intrepid Adversary")
            .withLandsOnBattlefield(1, "Plains", spare + 2)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Intrepid Adversary").error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        test("paying {1}{W} twice puts two valor counters on it and pumps your creatures") {
            val game = castAdversary(spare = 5)
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)

            val count = game.getPendingDecision() as? ChooseNumberDecision
                ?: error("expected the repeat-count question; got ${game.getPendingDecision()}")
            withClue("five spare Plains afford {1}{W} only twice") { count.maxValue shouldBe 2 }
            game.chooseNumber(2)
            game.resolveStack()

            game.valor() shouldBe 2
            val projected = game.state.projectedState
            val adversary = game.findPermanent("Intrepid Adversary")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            withClue("the anthem includes the Adversary itself") {
                projected.getPower(adversary) shouldBe 5
                projected.getToughness(adversary) shouldBe 3
            }
            projected.getPower(bears) shouldBe 4
            projected.getToughness(bears) shouldBe 4
            withClue("opponents' creatures are unaffected") {
                projected.getPower(game.findPermanent("Hill Giant")!!) shouldBe 3
            }
        }

        test("declining the payment leaves it a 3/1 with no counters") {
            val game = castAdversary(spare = 4)
            game.answerYesNo(false)
            game.resolveStack()

            game.valor() shouldBe 0
            val projected = game.state.projectedState
            projected.getPower(game.findPermanent("Intrepid Adversary")!!) shouldBe 3
            projected.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 2
        }

        test("no question when even one payment is unaffordable") {
            val game = castAdversary(spare = 1)
            withClue("one spare Plains can't pay {1}{W}") {
                (game.getPendingDecision() is YesNoDecision) shouldBe false
            }
            game.valor() shouldBe 0
        }
    }
}
