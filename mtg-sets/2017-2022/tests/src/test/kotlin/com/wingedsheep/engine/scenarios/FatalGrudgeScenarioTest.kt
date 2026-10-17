package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Fatal Grudge (SNC #187) — {B}{R} Sorcery.
 *
 *   As an additional cost to cast this spell, sacrifice a nonland permanent.
 *   Each opponent chooses a permanent they control that shares a card type with the sacrificed
 *   permanent and sacrifices it.
 *   Draw a card.
 *
 * The opponent's options are filtered by the card types of the permanent sacrificed as the cost,
 * read from its snapshot after it has already left the battlefield.
 */
class FatalGrudgeScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Fatal Grudge")
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardOnBattlefield(1, "Sol Ring")
        .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
        .withCardOnBattlefield(2, "Craw Wurm", summoningSickness = false)
        .withCardOnBattlefield(2, "Glorious Anthem")
        .withLandsOnBattlefield(1, "Swamp", 1)
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withCardInLibrary(1, "Forest")
        .withCardInLibrary(1, "Forest")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Fatal Grudge") {

            test("sacrificing a creature makes the opponent choose one of their creatures") {
                val game = board()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                val wurm = game.findPermanent("Craw Wurm")!!
                val card = game.findCardsInHand(1, "Fatal Grudge").first()
                val handBefore = game.handSize(1)

                game.execute(
                    CastSpell(
                        game.player1Id,
                        card,
                        additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(bears)),
                    )
                ).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("the opponent chooses among their creatures only, not the enchantment") {
                    (decision is SelectCardsDecision) shouldBe true
                    decision as SelectCardsDecision
                    decision.playerId shouldBe game.player2Id
                    decision.options.toSet() shouldBe setOf(giant, wurm)
                }
                game.selectCards(listOf(giant)).error shouldBe null

                withClue("the chosen creature is sacrificed, the other stays") {
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                    game.isOnBattlefield("Craw Wurm") shouldBe true
                    game.isOnBattlefield("Glorious Anthem") shouldBe true
                }
                withClue("the cost sacrificed Grizzly Bears") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("caster drew a card (hand: -Fatal Grudge, +1 draw)") {
                    game.handSize(1) shouldBe handBefore
                }
            }

            test("sacrificing an artifact spares an opponent with no artifacts, and still draws") {
                val game = board()
                val ring = game.findPermanent("Sol Ring")!!
                val card = game.findCardsInHand(1, "Fatal Grudge").first()
                val handBefore = game.handSize(1)

                game.execute(
                    CastSpell(
                        game.player1Id,
                        card,
                        additionalCostPayment = AdditionalCostPayment(sacrificedPermanents = listOf(ring)),
                    )
                ).error shouldBe null
                game.resolveStack()

                game.getPendingDecision() shouldBe null
                withClue("no opponent permanent shares a card type with an artifact") {
                    game.isOnBattlefield("Hill Giant") shouldBe true
                    game.isOnBattlefield("Craw Wurm") shouldBe true
                    game.isOnBattlefield("Glorious Anthem") shouldBe true
                }
                game.handSize(1) shouldBe handBefore
            }

            test("cannot be cast with only lands to sacrifice") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Fatal Grudge")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val card = game.findCardsInHand(1, "Fatal Grudge").first()

                game.execute(CastSpell(game.player1Id, card)).error shouldNotBe null
            }
        }
    }
}
