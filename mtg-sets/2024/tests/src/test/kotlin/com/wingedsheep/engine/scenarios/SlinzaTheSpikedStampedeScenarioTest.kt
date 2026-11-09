package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Slinza, the Spiked Stampede (J25 #55):
 *   Beast spells you cast cost {2} less to cast.
 *   Each other Beast creature you control enters with an additional +1/+1 counter on it.
 *   Whenever Slinza or another creature with power 4 or greater enters, you may pay {1}{R/G}.
 *   When you do, Slinza fights target creature you don't control.
 *
 * Exercises the "Slinza or another creature with power 4 or greater" union (Slinza itself is
 * matched by identity, the others by entry power including the replacement counter) feeding the
 * optional payment and its reflexive fight.
 */
class SlinzaTheSpikedStampedeScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusOneCounters(name: String): Int =
        state.getEntity(findPermanent(name)!!)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun TestGame.untappedForests(): Int =
        findPermanents("Forest").count { state.getEntity(it)?.has<TappedComponent>() != true }

    init {
        context("Slinza, the Spiked Stampede") {

            test("a cheaper Beast enters with a counter, reaches power 4, and Slinza fights") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Slinza, the Spiked Stampede")
                    .withCardInHand(1, "Snarling Undorak")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Snarling Undorak").error shouldBe null
                withClue("{2}{G}{G} Beast costs only {G}{G}") {
                    game.untappedForests() shouldBe 2
                }
                game.resolveStack()

                withClue("the other Beast enters with an additional +1/+1 counter, making it 4/4") {
                    game.plusOneCounters("Snarling Undorak") shouldBe 1
                    game.state.projectedState.getPower(game.findPermanent("Snarling Undorak")!!) shouldBe 4
                }

                withClue("the 4-power entry triggers Slinza's offer to pay") {
                    game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(true).error shouldBe null
                if (game.state.pendingDecision is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.untappedForests() shouldBe 0

                game.selectTargets(listOf(game.findPermanent("Hill Giant")!!)).error shouldBe null
                game.resolveStack()

                withClue("Slinza deals 5 to Hill Giant and takes 3 back") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                    val slinza = game.findPermanent("Slinza, the Spiked Stampede")!!
                    game.state.getEntity(slinza)?.get<DamageComponent>()?.amount shouldBe 3
                }
            }

            test("Slinza's own entry triggers, and declining the payment does nothing") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardInHand(1, "Slinza, the Spiked Stampede")
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Slinza, the Spiked Stampede").error shouldBe null
                game.resolveStack()

                withClue("Slinza is not an 'other' Beast, so it gets no counter of its own") {
                    game.plusOneCounters("Slinza, the Spiked Stampede") shouldBe 0
                }
                withClue("Slinza entering triggers its last ability") {
                    game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
                }
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                withClue("no mana paid, no fight") {
                    game.untappedForests() shouldBe 2
                    game.isOnBattlefield("Hill Giant") shouldBe true
                }
            }

            test("a non-Beast with power below 4 gets no counter and doesn't trigger") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Slinza, the Spiked Stampede")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Grizzly Bears").error shouldBe null
                withClue("a non-Beast spell isn't discounted") {
                    game.untappedForests() shouldBe 2
                }
                game.resolveStack()

                game.plusOneCounters("Grizzly Bears") shouldBe 0
                withClue("nothing triggered") {
                    game.state.pendingDecision shouldBe null
                    game.state.stack.isEmpty() shouldBe true
                }
            }
        }
    }
}
