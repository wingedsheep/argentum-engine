package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Too Evil to Stay Dead (MSH #118) — {2}{B} Sorcery.
 *
 *   Teamwork 4
 *   Choose target creature card in your graveyard with mana value 4 or less. If this spell was
 *   cast using teamwork, instead choose target creature card in your graveyard. Return the chosen
 *   card to the battlefield.
 *
 * "Instead choose" swaps the *target requirement*, so the two branches are two different
 * announcements. Hill Giant ({3}{R}, mana value 4) sits exactly on the plain threshold; Shivan
 * Dragon ({4}{R}{R}, mana value 6) is reachable only by the teamwork cast. Craw Wurm (6/4) is on
 * the battlefield purely to pay teamwork 4 on its own.
 */
class TooEvilToStayDeadScenarioTest : ScenarioTestBase() {

    init {
        context("Too Evil to Stay Dead") {

            test("cast without teamwork returns a mana value 4 or less creature card") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Too Evil to Stay Dead")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Shivan Dragon")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findCardsInGraveyard(1, "Hill Giant").first()
                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Too Evil to Stay Dead").first()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Card(giant, game.player1Id, Zone.GRAVEYARD)),
                    ),
                ).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe true
                withClue("Shivan Dragon was not chosen, so it stays in the graveyard") {
                    game.isInGraveyard(1, "Shivan Dragon") shouldBe true
                }
                withClue("no teamwork cost was declared, so nothing tapped") {
                    game.state.getEntity(wurm)?.has<TappedComponent>() shouldBe false
                }
            }

            test("the plain cast cannot target a creature card with mana value 5 or greater") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Too Evil to Stay Dead")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardInGraveyard(1, "Shivan Dragon")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dragon = game.findCardsInGraveyard(1, "Shivan Dragon").first()
                val cardId = game.findCardsInHand(1, "Too Evil to Stay Dead").first()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Card(dragon, game.player1Id, Zone.GRAVEYARD)),
                    ),
                ).error.shouldNotBeNull()
                game.isInHand(1, "Too Evil to Stay Dead") shouldBe true
                game.isInGraveyard(1, "Shivan Dragon") shouldBe true
            }

            test("cast using teamwork returns any creature card and taps the payers") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Too Evil to Stay Dead")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardInGraveyard(1, "Shivan Dragon")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dragon = game.findCardsInGraveyard(1, "Shivan Dragon").first()
                val wurm = game.findPermanent("Craw Wurm").shouldNotBeNull()
                val cardId = game.findCardsInHand(1, "Too Evil to Stay Dead").first()

                // Teamwork 4 — the 6/4 Craw Wurm clears the threshold on its own.
                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        targets = listOf(ChosenTarget.Card(dragon, game.player1Id, Zone.GRAVEYARD)),
                        declaredCostSlot = ChoiceSlot.TEAMWORK,
                        additionalCostPayment = AdditionalCostPayment(
                            variableCostPermanents = listOf(wurm),
                        ),
                    ),
                ).error shouldBe null
                game.state.getEntity(wurm)?.has<TappedComponent>() shouldBe true

                game.resolveStack()

                withClue("the teamwork branch has no mana value restriction") {
                    game.isOnBattlefield("Shivan Dragon") shouldBe true
                }
            }

            // Driving `CastSpell` directly proves only what the handler accepts. This one goes
            // against `getLegalActions`, which is what the client actually sees.
            test("the enumerator offers only the teamwork cast when the graveyard holds no cheap creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Too Evil to Stay Dead")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardInGraveyard(1, "Shivan Dragon")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val dragon = game.findCardsInGraveyard(1, "Shivan Dragon").first()
                val casts = game.getLegalActions(1)
                    .filter { it.description.startsWith("Cast Too Evil to Stay Dead") }

                withClue("mana value 4 or less has no legal target, so the plain cast is not offered") {
                    casts.none { it.actionType == "CastSpell" } shouldBe true
                }

                val teamworkCast = casts.single { it.actionType == "CastWithKicker" }
                teamworkCast.description shouldBe "Cast Too Evil to Stay Dead (Teamwork 4)"
                teamworkCast.isAffordable shouldBe true
                teamworkCast.additionalCostInfo?.costType shouldBe "TapForTotalPower"
                teamworkCast.validTargets.shouldNotBeNull() shouldContain dragon
            }

            // The mirror of the test above: with a legal cheap target present, both variants are
            // offered, and the plain one advertises the *narrow* list. This is the assertion that
            // catches a filter that silently widened.
            test("the enumerator offers both casts, with the plain one restricted to the cheap creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Too Evil to Stay Dead")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(1, "Craw Wurm")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Shivan Dragon")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findCardsInGraveyard(1, "Hill Giant").first()
                val dragon = game.findCardsInGraveyard(1, "Shivan Dragon").first()
                val casts = game.getLegalActions(1)
                    .filter { it.description.startsWith("Cast Too Evil to Stay Dead") }

                val plainCast = casts.single { it.actionType == "CastSpell" }
                withClue("the mana value 6 Shivan Dragon is outside the plain branch's filter") {
                    plainCast.validTargets shouldBe listOf(giant)
                }

                val teamworkCast = casts.single { it.actionType == "CastWithKicker" }
                withClue("the teamwork branch has no mana value restriction, so both are reachable") {
                    teamworkCast.validTargets.shouldNotBeNull()
                        .shouldContainExactlyInAnyOrder(giant, dragon)
                }
            }
        }
    }
}
