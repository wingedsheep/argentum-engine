package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.core.NumberChosenResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hazel of the Rootbloom (BLC #2).
 *
 * {T}, Pay 2 life, Tap X untapped tokens you control: Add X mana in any combination of colors.
 * At the beginning of your end step, create a token that's a copy of target token you control.
 * If that token is a Squirrel, instead create two tokens that are copies of it.
 *
 * Covers the first mana ability built on a Tap-X cost (X flows from the tapped tokens into the
 * pip-by-pip colour prompts; summoning-sick tokens may be tapped, per the ruling) and the
 * Squirrel/non-Squirrel branches of the end-step copy.
 */
class HazelOfTheRootbloomScenarioTest : ScenarioTestBase() {

    private fun TestGame.answerColors(vararg colors: Color) {
        for (color in colors) {
            val decision = getPendingDecision()
            decision.shouldBeInstanceOf<ChooseColorDecision>()
            submitDecision(ColorChosenResponse(decision.id, color)).error shouldBe null
        }
    }

    private fun TestGame.tokensNamed(name: String) =
        findPermanents(name).filter { state.getEntity(it)?.has<TokenComponent>() == true }

    init {
        context("Hazel of the Rootbloom") {
            test("mana ability taps X tokens, pays 2 life, and adds X mana in any colours") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Hazel of the Rootbloom")
                    .withCardOnBattlefield(1, "Grizzly Bears", isToken = true, summoningSickness = true)
                    .withCardOnBattlefield(1, "Grizzly Bears", isToken = true, summoningSickness = true)
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val hazel = game.findPermanent("Hazel of the Rootbloom")!!
                val bears = game.findPermanents("Grizzly Bears")
                val ability = cardRegistry.getCard("Hazel of the Rootbloom")!!.script.activatedAbilities[0]

                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = hazel,
                        abilityId = ability.id,
                        costPayment = AdditionalCostPayment(tappedPermanents = bears),
                        xValue = 2
                    )
                ).error shouldBe null
                game.answerColors(Color.BLACK, Color.WHITE)

                val pool = game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!
                pool.black shouldBe 1
                pool.white shouldBe 1
                game.getLifeTotal(1) shouldBe 18
                (bears + hazel).forEach { game.state.getEntity(it)!!.has<TappedComponent>() shouldBe true }
                game.state.getEntity(game.findPermanent("Llanowar Elves")!!)!!.has<TappedComponent>() shouldBe false
            }

            test("decision flow offers only tokens to tap") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Hazel of the Rootbloom")
                    .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
                    .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
                    .withCardOnBattlefield(1, "Llanowar Elves")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val hazel = game.findPermanent("Hazel of the Rootbloom")!!
                val bears = game.findPermanents("Grizzly Bears")
                val ability = cardRegistry.getCard("Hazel of the Rootbloom")!!.script.activatedAbilities[0]

                game.execute(ActivateAbility(game.player1Id, hazel, ability.id)).error shouldBe null
                val number = game.getPendingDecision()
                number.shouldBeInstanceOf<ChooseNumberDecision>()
                number.maxValue shouldBe 2
                game.submitDecision(NumberChosenResponse(number.id, 1)).error shouldBe null
                val select = game.getPendingDecision()
                select.shouldBeInstanceOf<SelectCardsDecision>()
                select.options shouldContainExactlyInAnyOrder bears
                game.submitDecision(CardsSelectedResponse(select.id, listOf(bears[0]))).error shouldBe null
                game.answerColors(Color.GREEN)

                game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.green shouldBe 1
                game.getLifeTotal(1) shouldBe 18
            }

            test("end step copies a Squirrel token twice") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Hazel of the Rootbloom")
                    .withCardOnBattlefield(1, "Thornvault Forager", isToken = true)
                    .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()
                val squirrel = game.findPermanent("Thornvault Forager")!!

                game.passUntilPhase(Phase.ENDING, Step.END)
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(squirrel)).error shouldBe null
                game.resolveStack()

                withClue("a Squirrel token is copied twice") {
                    game.tokensNamed("Thornvault Forager").size shouldBe 3
                    game.tokensNamed("Grizzly Bears").size shouldBe 1
                }
            }

            test("end step copies a non-Squirrel token once") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Hazel of the Rootbloom")
                    .withCardOnBattlefield(1, "Thornvault Forager", isToken = true)
                    .withCardOnBattlefield(1, "Grizzly Bears", isToken = true)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                    .build()
                val bear = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.ENDING, Step.END)
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                game.selectTargets(listOf(bear)).error shouldBe null
                game.resolveStack()

                withClue("a non-Squirrel token is copied once") {
                    game.tokensNamed("Grizzly Bears").size shouldBe 2
                    game.tokensNamed("Thornvault Forager").size shouldBe 1
                }
            }
        }
    }
}
