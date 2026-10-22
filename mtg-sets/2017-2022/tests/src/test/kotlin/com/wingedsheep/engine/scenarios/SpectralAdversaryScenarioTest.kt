package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Spectral Adversary (Innistrad: Midnight Hunt #77).
 *
 * When this creature enters, you may pay {1}{U} any number of times. When you pay this cost one
 * or more times, put that many +1/+1 counters on this creature, then up to that many other target
 * artifacts, creatures, and/or enchantments phase out.
 *
 * Proves the reflexive target cap reads the repetition count ([DynamicAmounts.timesPaid]).
 */
class SpectralAdversaryScenarioTest : ScenarioTestBase() {

    private fun TestGame.isPhasedOut(id: EntityId): Boolean =
        state.getEntity(id)?.get<PhasedOutComponent>() != null

    private fun TestGame.plusCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    /** Cast the Adversary off two Islands, leaving [spare] Islands untapped, and say yes. */
    private fun castAdversary(spare: Int): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Spectral Adversary")
            .withLandsOnBattlefield(1, "Island", spare + 2)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardOnBattlefield(2, "Glory Seeker")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Spectral Adversary").error shouldBe null
        game.resolveStack()
        game.answerYesNo(true)
        return game
    }

    init {
        test("paying twice gives two counters and phases out up to two other targets") {
            val game = castAdversary(spare = 4)
            val count = game.getPendingDecision() as? ChooseNumberDecision
                ?: error("expected the repeat-count question; got ${game.getPendingDecision()}")
            count.maxValue shouldBe 2
            game.chooseNumber(2)

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val seeker = game.findPermanent("Glory Seeker")!!
            withClue("three targets exceed the two payments") {
                game.selectTargets(listOf(bears, giant, seeker)).error shouldNotBe null
            }
            game.selectTargets(listOf(bears, giant)).error shouldBe null
            game.resolveStack()

            val adversary = game.findPermanent("Spectral Adversary")!!
            game.plusCounters(adversary) shouldBe 2
            game.isPhasedOut(bears) shouldBe true
            game.isPhasedOut(giant) shouldBe true
            game.isPhasedOut(seeker) shouldBe false
            game.isPhasedOut(adversary) shouldBe false
        }

        test("paying once allows only one target and the Adversary can't target itself") {
            val game = castAdversary(spare = 2)
            game.chooseNumber(1)

            val adversary = game.findPermanent("Spectral Adversary")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            withClue("\"other target\" excludes the Adversary") {
                game.selectTargets(listOf(adversary)).error shouldNotBe null
            }
            withClue("two targets exceed the single payment") {
                game.selectTargets(listOf(bears, giant)).error shouldNotBe null
            }
            game.selectTargets(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.plusCounters(adversary) shouldBe 1
            game.isPhasedOut(bears) shouldBe true
            game.isPhasedOut(giant) shouldBe false
        }
    }
}
