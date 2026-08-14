package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Widow's Bite (MSH #122) — {1}{B} Instant.
 *
 *   Teamwork 3
 *   Choose one. If this spell was cast using teamwork, choose both instead.
 *   • Target creature gains deathtouch until end of turn.
 *   • Target creature gets -2/-2 until end of turn.
 *
 * Pins all four branches of the modal teamwork shape: one mode on a plain cast, both modes on a
 * teamwork cast, and each cast asking for the other's mode count being rejected outright — "choose
 * both instead" is mandatory in both directions (CR 700.2, declared per CR 601.2b).
 */
class WidowsBiteScenarioTest : ScenarioTestBase() {

    init {
        context("Widow's Bite") {

            test("cast without teamwork resolves only the one chosen mode") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Widow's Bite")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                val courser = game.findPermanent("Centaur Courser").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Widow's Bite").first()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Permanent(courser)),
                        chosenModes = listOf(0),
                        modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(courser))),
                    ),
                ).error shouldBe null
                game.resolveStack()

                game.state.projectedState.hasKeyword(courser, Keyword.DEATHTOUCH) shouldBe true
                withClue("the -2/-2 mode was not chosen, so the 2/2 is untouched") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }
                withClue("no teamwork was declared, so nothing tapped") {
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe false
                }
            }

            test("cast using teamwork resolves both modes") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Widow's Bite")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                val courser = game.findPermanent("Centaur Courser").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Widow's Bite").first()

                // Teamwork 3 — the 3/3 Hill Giant clears the threshold on its own.
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Permanent(courser),
                            ChosenTarget.Permanent(bears),
                        ),
                        chosenModes = listOf(0, 1),
                        modeTargetsOrdered = listOf(
                            listOf(ChosenTarget.Permanent(courser)),
                            listOf(ChosenTarget.Permanent(bears)),
                        ),
                        declaredCostSlot = ChoiceSlot.TEAMWORK,
                        additionalCostPayment = AdditionalCostPayment(
                            variableCostPermanents = listOf(giant),
                        ),
                    ),
                ).error shouldBe null
                game.state.getEntity(giant)?.has<TappedComponent>() shouldBe true

                game.resolveStack()

                game.state.projectedState.hasKeyword(courser, Keyword.DEATHTOUCH) shouldBe true
                withClue("-2/-2 kills the 2/2") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                }
            }

            test("choosing one mode with teamwork declared is rejected") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Widow's Bite")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant").shouldNotBeNull()
                val courser = game.findPermanent("Centaur Courser").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Widow's Bite").first()

                // "Choose both instead" is not an allowance — a cast that declared teamwork owes
                // both modes, so a one-mode submission is illegal (CR 601.2, 700.2a).
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Permanent(courser)),
                        chosenModes = listOf(0),
                        modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(courser))),
                        declaredCostSlot = ChoiceSlot.TEAMWORK,
                        additionalCostPayment = AdditionalCostPayment(
                            variableCostPermanents = listOf(giant),
                        ),
                    ),
                ).error.shouldNotBeNull()

                withClue("the rejected cast is rewound whole — the card stays in hand and the " +
                    "creature tapped to pay for it is untapped again") {
                    game.isInHand(1, "Widow's Bite") shouldBe true
                    game.state.getEntity(giant)?.has<TappedComponent>() shouldBe false
                }
            }

            test("the enumerator advertises the teamwork variant as choose-exactly-two") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Widow's Bite")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val modalCasts = game.getLegalActions(1).filter { it.actionType == "CastSpellModal" }

                val plain = modalCasts
                    .firstOrNull { it.additionalCostInfo?.costType != "TapForTotalPower" }
                    .shouldNotBeNull()
                    .modalEnumeration.shouldNotBeNull()
                plain.minChooseCount shouldBe 1
                plain.chooseCount shouldBe 1

                val teamwork = modalCasts
                    .firstOrNull { it.additionalCostInfo?.costType == "TapForTotalPower" }
                    .shouldNotBeNull()
                    .modalEnumeration.shouldNotBeNull()
                withClue("both ends of the advertised range move with the declaration, so the " +
                    "client's confirm button can't unlock on a single mode") {
                    teamwork.minChooseCount shouldBe 2
                    teamwork.chooseCount shouldBe 2
                }
            }

            test("choosing both modes without teamwork is rejected") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Widow's Bite")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser").shouldNotBeNull()
                val bears = game.findPermanent("Grizzly Bears").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Widow's Bite").first()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(
                            ChosenTarget.Permanent(courser),
                            ChosenTarget.Permanent(bears),
                        ),
                        chosenModes = listOf(0, 1),
                        modeTargetsOrdered = listOf(
                            listOf(ChosenTarget.Permanent(courser)),
                            listOf(ChosenTarget.Permanent(bears)),
                        ),
                    ),
                ).error.shouldNotBeNull()
                game.isInHand(1, "Widow's Bite") shouldBe true
            }
        }
    }
}
