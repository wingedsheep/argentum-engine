package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Stir Up Trouble (HOB #84) — {B} Sorcery.
 *
 * "As an additional cost to cast this spell, sacrifice an artifact or creature or pay {4}.
 *  Destroy target creature."
 *
 * Exercises both legs of the sacrifice-or-pay additional cost. The sacrifice filter is "artifact
 * **or** creature", so an artifact alone is a legal payment — the case a plain "sacrifice a
 * creature" model would get wrong — and with no artifact and no creature the pay path must still
 * be offered so the spell is castable off an empty board.
 */
class StirUpTroubleScenarioTest : ScenarioTestBase() {

    private val testRelic = card("Stir Up Test Relic") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }
    private val testBear = card("Stir Up Test Bear") {
        manaCost = "{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    init {
        cardRegistry.register(listOf(testRelic, testBear))

        context("sacrifice-or-pay additional cost") {

            test("sacrificing an artifact pays the additional cost and destroys the target") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Stir Up Trouble")
                    .withCardOnBattlefield(1, "Stir Up Test Relic")
                    .withLandsOnBattlefield(1, "Swamp", 1) // only {B} — the pay path is unaffordable
                    .withCardOnBattlefield(2, "Stir Up Test Bear")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val relic = game.findPermanent("Stir Up Test Relic")!!
                val bear = game.findPermanent("Stir Up Test Bear")!!
                val spell = game.state.getHand(game.player1Id).first {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Stir Up Trouble"
                }

                val result = game.execute(
                    CastSpell(
                        game.player1Id,
                        spell,
                        listOf(ChosenTarget.Permanent(bear)),
                        additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(relic)),
                    )
                )
                withClue("an artifact should satisfy 'sacrifice an artifact or creature': ${result.error}") {
                    result.error shouldBe null
                }
                withClue("the artifact was sacrificed as a cost") {
                    game.isInGraveyard(1, "Stir Up Test Relic") shouldBe true
                }

                game.resolveStack()

                withClue("the targeted creature is destroyed") {
                    game.isOnBattlefield("Stir Up Test Bear") shouldBe false
                    game.isInGraveyard(2, "Stir Up Test Bear") shouldBe true
                }
            }

            test("the pay path casts it off an empty board for {4}{B}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Stir Up Trouble")
                    .withLandsOnBattlefield(1, "Swamp", 5) // {B} + {4}
                    .withCardOnBattlefield(2, "Stir Up Test Bear")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bear = game.findPermanent("Stir Up Test Bear")!!

                withClue("no sacrifice payment — the engine adds {4} to the cost") {
                    game.castSpell(1, "Stir Up Trouble", bear).error shouldBe null
                }
                game.resolveStack()

                withClue("the targeted creature is destroyed") {
                    game.isInGraveyard(2, "Stir Up Test Bear") shouldBe true
                }
            }

            test("one Swamp and nothing to sacrifice leaves it uncastable") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Stir Up Trouble")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardOnBattlefield(2, "Stir Up Test Bear")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("neither leg of the additional cost is payable") {
                    game.getLegalActions(1)
                        .none { it.description.startsWith("Cast Stir Up Trouble") } shouldBe true
                }

                val bear = game.findPermanent("Stir Up Test Bear")!!
                withClue("and casting it anyway is rejected") {
                    game.castSpell(1, "Stir Up Trouble", bear).error shouldNotBe null
                }
            }
        }
    }
}
