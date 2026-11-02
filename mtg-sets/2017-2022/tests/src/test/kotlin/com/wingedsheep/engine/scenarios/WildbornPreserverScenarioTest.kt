package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Wildborn Preserver (ELD #182).
 *
 * {1}{G} Creature — Elf Archer 2/2
 * "Flash, reach
 *  Whenever another non-Human creature you control enters, you may pay {X}. When you do, put X
 *  +1/+1 counters on this creature."
 *
 * Covers the pay-{X} reflexive payoff, the non-Human filter, and declining the payment.
 */
class WildbornPreserverScenarioTest : ScenarioTestBase() {

    private fun plusOneCounters(game: TestGame): Int {
        val preserver = game.findPermanent("Wildborn Preserver")!!
        return game.state.getEntity(preserver)?.get<CountersComponent>()
            ?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    init {
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Beast",
                manaCost = ManaCost.parse("{1}{G}"),
                subtypes = setOf(Subtype("Beast")),
                power = 2,
                toughness = 2
            )
        )
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Villager",
                manaCost = ManaCost.parse("{1}{G}"),
                subtypes = setOf(Subtype.HUMAN),
                power = 2,
                toughness = 2
            )
        )

        context("Wildborn Preserver") {

            test("paying {X} puts X +1/+1 counters on the Preserver") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Wildborn Preserver")
                    .withCardInHand(1, "Test Beast")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Beast").error shouldBe null
                game.resolveStack()

                game.answerYesNo(true).error shouldBe null
                val decision = game.getPendingDecision()
                (decision is ChooseNumberDecision) shouldBe true
                game.chooseNumber(3).error shouldBe null
                game.resolveStack()

                plusOneCounters(game) shouldBe 3
            }

            test("declining the payment leaves the Preserver untouched") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Wildborn Preserver")
                    .withCardInHand(1, "Test Beast")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Beast").error shouldBe null
                game.resolveStack()

                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                plusOneCounters(game) shouldBe 0
            }

            test("a Human creature entering does not trigger the ability") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Wildborn Preserver")
                    .withCardInHand(1, "Test Villager")
                    .withLandsOnBattlefield(1, "Forest", 8)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Test Villager").error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                plusOneCounters(game) shouldBe 0
            }

            test("a non-Human creature an opponent controls does not trigger the ability") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Wildborn Preserver")
                    .withCardInHand(2, "Test Beast")
                    .withLandsOnBattlefield(2, "Forest", 8)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Test Beast").error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                plusOneCounters(game) shouldBe 0
            }
        }
    }
}
