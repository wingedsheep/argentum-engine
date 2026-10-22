package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Tainted Adversary (Innistrad: Midnight Hunt #124).
 *
 * When this creature enters, you may pay {2}{B} any number of times. When you pay this cost one
 * or more times, put that many +1/+1 counters on this creature, then create twice that many 2/2
 * black Zombie creature tokens with decayed.
 */
class TaintedAdversaryScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOnes(): Int =
        state.getEntity(findPermanent("Tainted Adversary")!!)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Cast the Adversary off two Swamps, leaving [spare] Swamps untapped. */
    private fun castAdversary(spare: Int): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Tainted Adversary")
            .withLandsOnBattlefield(1, "Swamp", spare + 2)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Swamp")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Tainted Adversary").error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        test("paying {2}{B} twice puts two +1/+1 counters on it and creates four decayed Zombies") {
            val game = castAdversary(spare = 7)
            (game.getPendingDecision() is YesNoDecision) shouldBe true
            game.answerYesNo(true)

            val count = game.getPendingDecision() as? ChooseNumberDecision
                ?: error("expected the repeat-count question; got ${game.getPendingDecision()}")
            withClue("seven spare Swamps afford {2}{B} only twice") { count.maxValue shouldBe 2 }
            game.chooseNumber(2)
            game.resolveStack()

            game.plusOnes() shouldBe 2
            val zombies = game.findPermanents("Zombie Token")
            withClue("twice the number of payments") { zombies.size shouldBe 4 }
            val projected = game.state.projectedState
            zombies.forEach { zombie ->
                projected.getPower(zombie) shouldBe 2
                projected.getToughness(zombie) shouldBe 2
                projected.hasKeyword(zombie, Keyword.DECAYED) shouldBe true
            }
        }

        test("declining the payment leaves it a 2/3 with no counters and no tokens") {
            val game = castAdversary(spare = 3)
            game.answerYesNo(false)
            game.resolveStack()

            game.plusOnes() shouldBe 0
            game.findPermanents("Zombie Token").size shouldBe 0
        }
    }
}
