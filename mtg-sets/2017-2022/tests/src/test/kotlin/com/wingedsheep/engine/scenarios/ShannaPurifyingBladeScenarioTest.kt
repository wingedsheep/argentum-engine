package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Shanna, Purifying Blade (DMU #218).
 *
 * "Lifelink
 *  At the beginning of your end step, you may pay {X}. If you do, draw X cards. X can't be greater
 *  than the amount of life you gained this turn."
 *
 * The capped payment is composed (choose X up to life gained → may pay {X} → draw X), so these
 * prove the cap, that the chosen X survives the payment pause into the draw, and the no-life case.
 */
class ShannaPurifyingBladeScenarioTest : ScenarioTestBase() {

    private val gainThreeLife = card("Test Gain Three") {
        manaCost = "{W}"
        typeLine = "Sorcery"
        spell { effect = Effects.GainLife(3) }
    }

    private fun ScenarioTestBase.TestGame.payPendingCost() {
        repeat(3) {
            when (getPendingDecision()) {
                is YesNoDecision -> answerYesNo(true)
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay()
                else -> return
            }
        }
    }

    private fun ScenarioTestBase.TestGame.tappedIslands(): Int =
        findPermanents("Island").count { state.getEntity(it)?.has<TappedComponent>() == true }

    private fun build(gainLife: Boolean): ScenarioTestBase.TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Shanna, Purifying Blade", summoningSickness = false)
            .withLandsOnBattlefield(1, "Plains", 1)
            .withLandsOnBattlefield(1, "Island", 5)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        repeat(6) { builder.withCardInLibrary(1, "Island") }
        repeat(3) { builder.withCardInLibrary(2, "Island") }
        if (gainLife) builder.withCardInHand(1, "Test Gain Three")
        return builder.build()
    }

    init {
        cardRegistry.register(gainThreeLife)

        context("Shanna, Purifying Blade") {

            test("X is capped by life gained; paying {2} draws two cards") {
                val game = build(gainLife = true)
                val cast = game.castSpell(1, "Test Gain Three")
                withClue("Casting the life-gain spell should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseNumberDecision>()
                withClue("X can't exceed the 3 life gained, even with 5 untapped Islands") {
                    decision.maxValue shouldBe 3
                }

                val handBefore = game.handSize(1)
                game.chooseNumber(2)
                game.payPendingCost()

                withClue("Paying {2} draws two cards") { game.handSize(1) shouldBe handBefore + 2 }
                withClue("Exactly two Islands were tapped to pay {X}") { game.tappedIslands() shouldBe 2 }
            }

            test("choosing X = 0 declines: no payment, no draw") {
                val game = build(gainLife = true)
                game.castSpell(1, "Test Gain Three")
                game.resolveStack()

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                val handBefore = game.handSize(1)
                game.getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
                game.chooseNumber(0)

                withClue("Nothing further is asked") { game.hasPendingDecision() shouldBe false }
                game.handSize(1) shouldBe handBefore
                game.tappedIslands() shouldBe 0
            }

            test("no life gained this turn: no prompt and no draw") {
                val game = build(gainLife = false)
                val handBefore = game.handSize(1)

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("With no life gained X can only be 0, so nothing is asked") {
                    game.hasPendingDecision() shouldBe false
                }
                game.handSize(1) shouldBe handBefore
                game.tappedIslands() shouldBe 0
            }
        }
    }
}
