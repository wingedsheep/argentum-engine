package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Bladecoil Serpent (BRO #229).
 *
 *   When this creature enters, for each {U}{U} spent to cast it, draw a card.
 *   When this creature enters, for each {B}{B} spent to cast it, each opponent discards a card.
 *   When this creature enters, for each {R}{R} spent to cast it, it gets +1/+0 and gains trample
 *   and haste until end of turn.
 *
 * Every colored mana counts, wherever it went — {X} and the generic {6} alike — and each trigger
 * counts *pairs*, so an odd leftover mana of a color does nothing. Each scenario taps exactly as many
 * lands as the cost needs, so the colors spent are the lands on the board.
 */
class BladecoilSerpentScenarioTest : ScenarioTestBase() {

    private fun TestGame.castSerpent(x: Int) {
        castXSpell(1, "Bladecoil Serpent", xValue = x).error shouldBe null
        resolveStack()
        // The opponent's discard (a choice) and trigger ordering may each pause resolution.
        var guard = 0
        while (hasPendingDecision() && guard++ < 10) {
            when (val decision = getPendingDecision()!!) {
                is OrderObjectsDecision -> submitDecision(OrderedResponse(decision.id, decision.objects))
                is SelectCardsDecision -> selectCards(decision.options.take(decision.minSelections))
                else -> error("unexpected decision ${decision::class.simpleName}")
            }
            resolveStack()
        }
    }

    init {
        context("Bladecoil Serpent") {

            test("one pair of each of {U}, {B} and {R} — paid partly through X — fires all three triggers once") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bladecoil Serpent")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardsInHand(2, "Grizzly Bears", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSerpent(x = 1)

                val serpent = game.findPermanent("Bladecoil Serpent")!!
                val projected = game.state.projectedState
                withClue("three {U} is one {U}{U}: draw one card") { game.handSize(1) shouldBe 1 }
                withClue("{B}{B}: the opponent discards one card") { game.handSize(2) shouldBe 1 }
                withClue("{R}{R}: +1/+0") { projected.getPower(serpent) shouldBe 6 }
                projected.getToughness(serpent) shouldBe 4
                projected.hasKeyword(serpent, Keyword.TRAMPLE) shouldBe true
                projected.hasKeyword(serpent, Keyword.HASTE) shouldBe true
            }

            test("four {R} is two pairs: +2/+0; a lone {U} and {B} do nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bladecoil Serpent")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardsInHand(2, "Grizzly Bears", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSerpent(x = 0)

                val serpent = game.findPermanent("Bladecoil Serpent")!!
                val projected = game.state.projectedState
                withClue("one {U} rounds down to no card") { game.handSize(1) shouldBe 0 }
                withClue("one {B} rounds down to no discard") { game.handSize(2) shouldBe 2 }
                projected.getPower(serpent) shouldBe 7
                projected.hasKeyword(serpent, Keyword.TRAMPLE) shouldBe true
            }

            test("no {R}{R} spent: no bonus and no trample or haste") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bladecoil Serpent")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardsInHand(2, "Grizzly Bears", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSerpent(x = 0)

                val serpent = game.findPermanent("Bladecoil Serpent")!!
                val projected = game.state.projectedState
                projected.getPower(serpent) shouldBe 5
                projected.hasKeyword(serpent, Keyword.TRAMPLE) shouldBe false
                projected.hasKeyword(serpent, Keyword.HASTE) shouldBe false
                game.handSize(2) shouldBe 2
            }
        }
    }
}
