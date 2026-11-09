package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Kaya's Guile (MH1 #205) — {1}{W}{B} Instant.
 *
 *   Choose two —
 *   • Each opponent sacrifices a creature of their choice.
 *   • Exile all opponents' graveyards.
 *   • Create a 1/1 white and black Spirit creature token with flying.
 *   • You gain 4 life.
 *   Entwine {3}
 *
 * Entwine (CR 702.42a) swaps "the number specified" for *all* modes and costs one extra {3} —
 * not {3} per extra mode — so the legal mode counts are exactly {2, 4}.
 */
class KayasGuileScenarioTest : ScenarioTestBase() {

    // Mode indices in printed order.
    private val sacrifice = 0
    private val exileGraveyards = 1
    private val spirit = 2
    private val gainLife = 3

    private fun ScenarioBuilder.board(lands: Int): ScenarioBuilder =
        withPlayers("Player1", "Player2")
            .withCardInHand(1, "Kaya's Guile")
            .withLandsOnBattlefield(1, "Plains", 1)
            .withLandsOnBattlefield(1, "Swamp", 1)
            .withLandsOnBattlefield(1, "Forest", lands - 2)
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInGraveyard(2, "Shock")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        context("Kaya's Guile") {

            test("without entwine, choosing two modes resolves exactly those two") {
                val game = scenario().board(lands = 3).build()
                val cardId = game.findCardsInHand(1, "Kaya's Guile").first()

                game.execute(
                    CastSpell(playerId = game.player1Id, cardId = cardId, chosenModes = listOf(spirit, gainLife))
                ).error shouldBe null
                game.resolveStack()

                game.findPermanents("Spirit Token").size shouldBe 1
                game.getLifeTotal(1) shouldBe 24
                withClue("the sacrifice and exile modes were not chosen") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Shock") shouldBe true
                }
            }

            test("without entwine, the sacrifice and graveyard-exile modes together") {
                val game = scenario().board(lands = 3).build()
                val cardId = game.findCardsInHand(1, "Kaya's Guile").first()

                game.execute(
                    CastSpell(playerId = game.player1Id, cardId = cardId, chosenModes = listOf(sacrifice, exileGraveyards))
                ).error shouldBe null
                game.resolveStack()

                withClue("modes resolve in printed order: the sacrificed creature lands in the graveyard first, " +
                    "then the second mode exiles it") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                    game.isInExile(2, "Shock") shouldBe true
                }
                game.getLifeTotal(1) shouldBe 20
                game.findPermanents("Spirit Token").size shouldBe 0
            }

            test("entwined, all four modes resolve for a single {3}") {
                // Exactly six lands: {1}{W}{B} plus one {3}. Escalate-style pricing ({3} per extra
                // mode) would need twelve.
                val game = scenario().board(lands = 6).build()
                val cardId = game.findCardsInHand(1, "Kaya's Guile").first()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        chosenModes = listOf(sacrifice, exileGraveyards, spirit, gainLife),
                        declaredCostSlot = ChoiceSlot.ENTWINED,
                    )
                ).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isInExile(2, "Shock") shouldBe true
                game.findPermanents("Spirit Token").size shouldBe 1
                game.getLifeTotal(1) shouldBe 24
            }

            test("entwining costs {3} more — five lands are not enough") {
                val game = scenario().board(lands = 5).build()
                val cardId = game.findCardsInHand(1, "Kaya's Guile").first()

                game.execute(
                    CastSpell(
                        playerId = game.player1Id,
                        cardId = cardId,
                        chosenModes = listOf(sacrifice, exileGraveyards, spirit, gainLife),
                        declaredCostSlot = ChoiceSlot.ENTWINED,
                    )
                ).error.shouldNotBeNull()
                game.isInHand(1, "Kaya's Guile") shouldBe true
            }

            test("three modes is never legal, entwined or not") {
                val game = scenario().board(lands = 6).build()
                val cardId = game.findCardsInHand(1, "Kaya's Guile").first()

                withClue("not entwined: exactly two") {
                    game.execute(
                        CastSpell(
                            playerId = game.player1Id,
                            cardId = cardId,
                            chosenModes = listOf(sacrifice, spirit, gainLife),
                        )
                    ).error.shouldNotBeNull()
                }
                withClue("entwined: all four") {
                    game.execute(
                        CastSpell(
                            playerId = game.player1Id,
                            cardId = cardId,
                            chosenModes = listOf(sacrifice, spirit, gainLife),
                            declaredCostSlot = ChoiceSlot.ENTWINED,
                        )
                    ).error.shouldNotBeNull()
                }
                game.isInHand(1, "Kaya's Guile") shouldBe true
            }

            test("one mode, four modes without entwine, and two modes entwined are all rejected") {
                val game = scenario().board(lands = 6).build()
                val cardId = game.findCardsInHand(1, "Kaya's Guile").first()

                withClue("one mode") {
                    game.execute(
                        CastSpell(playerId = game.player1Id, cardId = cardId, chosenModes = listOf(gainLife))
                    ).error.shouldNotBeNull()
                }
                withClue("four modes need the entwine cost") {
                    game.execute(
                        CastSpell(
                            playerId = game.player1Id,
                            cardId = cardId,
                            chosenModes = listOf(sacrifice, exileGraveyards, spirit, gainLife),
                        )
                    ).error.shouldNotBeNull()
                }
                withClue("entwine means all modes, not two") {
                    game.execute(
                        CastSpell(
                            playerId = game.player1Id,
                            cardId = cardId,
                            chosenModes = listOf(spirit, gainLife),
                            declaredCostSlot = ChoiceSlot.ENTWINED,
                        )
                    ).error.shouldNotBeNull()
                }
                withClue("choosing the same mode twice is not choosing two modes (CR 700.2d)") {
                    game.execute(
                        CastSpell(playerId = game.player1Id, cardId = cardId, chosenModes = listOf(gainLife, gainLife))
                    ).error.shouldNotBeNull()
                }
            }

            test("the enumerator advertises two modes plainly and all four when entwined") {
                val game = scenario().board(lands = 6).build()

                val modalCasts = game.getLegalActions(1).filter { it.actionType == "CastSpellModal" }

                val plain = modalCasts.firstOrNull { it.description == "Cast Kaya's Guile" }.shouldNotBeNull()
                val plainModal = plain.modalEnumeration.shouldNotBeNull()
                withClue("the plain cast must not advertise a count the handler rejects") {
                    plainModal.chooseCount shouldBe 2
                    plainModal.minChooseCount shouldBe 2
                }

                val entwined = modalCasts.firstOrNull { it.description == "Cast Kaya's Guile (Entwined)" }.shouldNotBeNull()
                val entwinedModal = entwined.modalEnumeration.shouldNotBeNull()
                entwinedModal.chooseCount shouldBe 4
                entwinedModal.minChooseCount shouldBe 4
                entwinedModal.modes.map { it.available } shouldBe listOf(true, true, true, true)
                entwined.isAffordable shouldBe true
            }

            test("the entwined variant is offered but unaffordable with only three lands") {
                val game = scenario().board(lands = 3).build()

                val modalCasts = game.getLegalActions(1).filter { it.actionType == "CastSpellModal" }

                modalCasts.first { it.description == "Cast Kaya's Guile" }.isAffordable shouldBe true
                val entwined = modalCasts.firstOrNull { it.description == "Cast Kaya's Guile (Entwined)" }
                withClue("an unaffordable variant is either absent or flagged unaffordable") {
                    (entwined == null || !entwined.isAffordable) shouldBe true
                }
            }
        }
    }
}
