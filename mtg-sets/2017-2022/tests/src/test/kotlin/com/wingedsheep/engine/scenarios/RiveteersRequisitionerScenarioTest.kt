package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Riveteers Requisitioner (SNC #121) — {1}{R} Creature — Lizard Rogue 3/1
 *
 *   When this creature dies, create a Treasure token.
 *   Blitz {2}{R}
 *
 * Blitzed, it attacks with haste and, when sacrificed at the end step, both its own dies trigger
 * (a Treasure) and the blitz one (a card) fire.
 */
class RiveteersRequisitionerScenarioTest : ScenarioTestBase() {

    init {
        context("Riveteers Requisitioner") {

            test("blitzed: has haste, and the end-step sacrifice makes a Treasure and draws a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Riveteers Requisitioner")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val card = game.findCardsInHand(1, "Riveteers Requisitioner").single()
                val result = game.execute(
                    CastSpell(
                        game.player1Id, card,
                        useAlternativeCost = true, alternativeCostType = AlternativeCostType.BLITZ
                    )
                )
                withClue("blitz cast should succeed: ${result.error}") { result.error shouldBe null }
                game.resolveStack()

                val permanent = game.findPermanent("Riveteers Requisitioner")!!
                game.state.projectedState.hasKeyword(permanent, Keyword.HASTE) shouldBe true

                val handBefore = game.handSize(1)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()
                // Its two dies triggers (Treasure, blitz draw) are ordered by their controller.
                (game.state.pendingDecision as? OrderObjectsDecision)?.let {
                    game.submitDecision(OrderedResponse(it.id, it.objects))
                }
                game.resolveStack()

                game.isInGraveyard(1, "Riveteers Requisitioner") shouldBe true
                game.findPermanents("Treasure").size shouldBe 1
                game.handSize(1) shouldBe handBefore + 1
            }

            test("cast for its normal cost, it dies without drawing a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Riveteers Requisitioner")
                    .withCardInHand(1, "Lightning Bolt")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Riveteers Requisitioner").error shouldBe null
                game.resolveStack()
                val permanent = game.findPermanent("Riveteers Requisitioner")!!
                game.state.projectedState.hasKeyword(permanent, Keyword.HASTE) shouldBe false

                game.castSpell(1, "Lightning Bolt", permanent).error shouldBe null
                val handBefore = game.handSize(1)
                game.resolveStack()

                game.isInGraveyard(1, "Riveteers Requisitioner") shouldBe true
                game.findPermanents("Treasure").size shouldBe 1
                game.handSize(1) shouldBe handBefore
            }
        }
    }
}
