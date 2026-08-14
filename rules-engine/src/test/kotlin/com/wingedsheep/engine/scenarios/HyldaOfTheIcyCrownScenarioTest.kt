package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Hylda of the Icy Crown (WOE #206) — {2}{W}{U} Legendary Creature 3/4.
 *
 *   Whenever you tap an untapped creature an opponent controls, you may pay {1}. When you do,
 *   choose one —
 *   • Create a 4/4 white and blue Elemental creature token.
 *   • Put a +1/+1 counter on each creature you control.
 *   • Scry 2, then draw a card.
 *
 * The tap trigger is fed by Hylda's Crown of Winter (a {T} tapper you control). Each mode is
 * exercised once, plus the two ways the payoff can be skipped: declining the {1}, and a tap that
 * isn't yours.
 */
class HyldaOfTheIcyCrownScenarioTest : ScenarioTestBase() {

    private fun ScenarioBuilder.hyldaBoard(): ScenarioBuilder = this
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Hylda of the Icy Crown", summoningSickness = false)
        .withCardOnBattlefield(1, "Hylda's Crown of Winter")
        .withLandsOnBattlefield(1, "Island", 2)
        .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    /** Tap the opponent's Hill Giant with the Crown, then run the trigger picking [mode]. */
    private fun TestGame.tapWithCrownAndChoose(mode: Int?, pay: Boolean = true) {
        val crown = findPermanent("Hylda's Crown of Winter")!!
        val giant = findPermanent("Hill Giant")!!
        execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = crown,
                abilityId = cardRegistry.getCard("Hylda's Crown of Winter")!!
                    .activatedAbilities[0].id,
                targets = listOf(ChosenTarget.Permanent(giant)),
            )
        ).error shouldBe null

        var guard = 0
        while (guard++ < 40) {
            when (val decision = state.pendingDecision) {
                is YesNoDecision -> answerYesNo(pay)
                is SelectManaSourcesDecision -> submitManaSourcesAutoPay()
                is ChooseOptionDecision -> submitDecision(
                    OptionChosenResponse(
                        decision.id,
                        mode ?: error("a mode was offered but the test expected none")
                    )
                )
                // Scry: put nothing on the bottom, keep the looked-at cards in order.
                is SelectCardsDecision -> skipSelection()
                is ReorderLibraryDecision -> keepLibraryOrder()
                null -> {
                    if (state.stack.isEmpty()) return
                    resolveStack()
                }
                else -> error("unexpected decision: $decision")
            }
        }
        error("decision loop did not settle")
    }

    private fun TestGame.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    init {
        context("Hylda of the Icy Crown") {

            test("mode 1 creates a 4/4 white and blue Elemental token") {
                val game = scenario().hyldaBoard().build()

                game.tapWithCrownAndChoose(mode = 0)

                val token = game.findPermanent("Elemental Token")
                withClue("the token was created") { (token != null) shouldBe true }
                game.state.projectedState.getPower(token!!) shouldBe 4
                game.state.projectedState.getToughness(token) shouldBe 4
                withClue("the opposing creature really is tapped") {
                    game.state.getEntity(game.findPermanent("Hill Giant")!!)
                        ?.has<TappedComponent>() shouldBe true
                }
            }

            test("mode 2 puts a +1/+1 counter on each creature you control") {
                val game = scenario().hyldaBoard()
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .build()

                val hylda = game.findPermanent("Hylda of the Icy Crown")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.tapWithCrownAndChoose(mode = 1)

                withClue("every creature you control gets one counter") {
                    game.plusOneCounters(hylda) shouldBe 1
                    game.plusOneCounters(bears) shouldBe 1
                }
                withClue("the opponent's creature gets nothing") {
                    game.plusOneCounters(giant) shouldBe 0
                }
            }

            test("mode 3 scries 2 and draws a card") {
                val game = scenario().hyldaBoard()
                    .withCardInLibrary(1, "Craw Wurm")
                    .withCardInLibrary(1, "Bog Imp")
                    .withCardInLibrary(1, "Wind Drake")
                    .build()

                val handBefore = game.handSize(1)
                val libraryBefore = game.librarySize(1)

                game.tapWithCrownAndChoose(mode = 2)

                withClue("scry keeps the cards in the library; only the draw moves one") {
                    game.handSize(1) shouldBe handBefore + 1
                    game.librarySize(1) shouldBe libraryBefore - 1
                }
            }

            test("declining the {1} skips the mode choice entirely") {
                val game = scenario().hyldaBoard().build()

                game.tapWithCrownAndChoose(mode = null, pay = false)

                withClue("the tap happened but nothing was paid, so no mode resolved") {
                    game.state.getEntity(game.findPermanent("Hill Giant")!!)
                        ?.has<TappedComponent>() shouldBe true
                    game.findPermanent("Elemental Token") shouldBe null
                    game.plusOneCounters(game.findPermanent("Hylda of the Icy Crown")!!) shouldBe 0
                }
            }

            test("an opponent tapping their own creature never offers the payment") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hylda of the Icy Crown", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardOnBattlefield(2, "Hylda's Crown of Winter")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val crown = game.findPermanent("Hylda's Crown of Winter")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.execute(
                    ActivateAbility(
                        playerId = game.player2Id,
                        sourceId = crown,
                        abilityId = cardRegistry.getCard("Hylda's Crown of Winter")!!
                            .activatedAbilities[0].id,
                        targets = listOf(ChosenTarget.Permanent(giant)),
                    )
                ).error shouldBe null
                game.resolveStack()

                withClue("no decision at all — the trigger never fired") {
                    game.state.pendingDecision shouldBe null
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true
                    game.findPermanent("Elemental Token") shouldBe null
                }
            }
        }
    }
}
