package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Go Nuts! (MSH #168) — {G} Sorcery.
 *
 *   Teamwork 3
 *   Choose one. If this spell was cast using teamwork, choose both instead.
 *   • Put a +1/+1 counter on target creature.
 *   • Target creature you control fights target creature an opponent controls.
 *
 * The teamwork case deliberately points both modes at the same 2/2: the counter lands first, so it
 * is a 3/3 that trades with the opposing 3/3 — which pins that the modes resolve in printed order
 * within one resolution.
 */
class GoNutsScenarioTest : ScenarioTestBase() {

    init {
        context("Go Nuts!") {

            test("cast without teamwork resolves only the one chosen mode") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Go Nuts!")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Go Nuts!").first()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                        chosenModes = listOf(0),
                        modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(bears))),
                    ),
                ).error shouldBe null
                game.resolveStack()

                game.state.projectedState.getPower(bears) shouldBe 3
                game.state.projectedState.getToughness(bears) shouldBe 3
                withClue("the fight mode was not chosen, so nobody took damage") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isOnBattlefield("Centaur Courser") shouldBe true
                }
                withClue("no teamwork was declared, so nothing tapped") {
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe false
                }
            }

            test("cast using teamwork resolves both modes, counter before fight") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Go Nuts!")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val courser = game.findPermanent("Centaur Courser").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Go Nuts!").first()

                // Teamwork 3 — the 3/3 Hill Giant clears the threshold on its own.
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Permanent(bears),
                            ChosenTarget.Permanent(bears),
                            ChosenTarget.Permanent(courser),
                        ),
                        chosenModes = listOf(0, 1),
                        modeTargetsOrdered = listOf(
                            listOf(ChosenTarget.Permanent(bears)),
                            listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(courser)),
                        ),
                        declaredCostSlot = ChoiceSlot.TEAMWORK,
                        additionalCostPayment = AdditionalCostPayment(
                            variableCostPermanents = listOf(giant),
                        ),
                    ),
                ).error shouldBe null
                game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true

                game.resolveStack()

                withClue("the counter made the Bears a 3/3, so it trades with the 3/3 Courser") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isOnBattlefield("Centaur Courser") shouldBe false
                }
            }

            test("choosing one mode with teamwork declared is rejected") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Go Nuts!")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Go Nuts!").first()

                // "Choose both instead" is not an allowance — a cast that declared teamwork owes
                // both modes, so a one-mode submission is illegal (CR 601.2, 700.2a).
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Permanent(bears)),
                        chosenModes = listOf(0),
                        modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(bears))),
                        declaredCostSlot = ChoiceSlot.TEAMWORK,
                        additionalCostPayment = AdditionalCostPayment(
                            variableCostPermanents = listOf(giant),
                        ),
                    ),
                ).error.shouldNotBeNull()

                withClue("the rejected cast is rewound whole — no counter landed either") {
                    game.isInHand(1, "Go Nuts!") shouldBe true
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe false
                    game.state.projectedState.getPower(bears) shouldBe 2
                }
            }

            test("choosing both modes without teamwork is rejected") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Go Nuts!")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val courser = game.findPermanent("Centaur Courser").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Go Nuts!").first()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Permanent(bears),
                            ChosenTarget.Permanent(bears),
                            ChosenTarget.Permanent(courser),
                        ),
                        chosenModes = listOf(0, 1),
                        modeTargetsOrdered = listOf(
                            listOf(ChosenTarget.Permanent(bears)),
                            listOf(ChosenTarget.Permanent(bears), ChosenTarget.Permanent(courser)),
                        ),
                    ),
                ).error.shouldNotBeNull()
                game.isInHand(1, "Go Nuts!") shouldBe true
            }
        }
    }
}
