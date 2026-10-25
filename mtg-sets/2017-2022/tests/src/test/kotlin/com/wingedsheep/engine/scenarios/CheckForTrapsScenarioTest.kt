package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Check for Traps (AFR #92) — "Target opponent reveals their hand. You choose a nonland card from
 * it. Exile that card. If an instant card or a card with flash is exiled this way, they lose 1
 * life. Otherwise, you lose 1 life."
 *
 * The branch reads the card that was actually exiled: an instant or a flash card costs the
 * opponent life, anything else (or nothing exiled at all) costs the caster.
 */
class CheckForTrapsScenarioTest : ScenarioTestBase() {

    private fun castAndPick(cardInOpponentHand: List<String>, pick: String?): ScenarioTestBase.TestGame {
        val builder = scenario()
            .withPlayers("Alice", "Bob")
            .withCardInHand(1, "Check for Traps")
            .withLandsOnBattlefield(1, "Swamp", 2)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        cardInOpponentHand.forEach { builder.withCardInHand(2, it) }
        val game = builder.build()

        game.castSpellTargetingPlayer(1, "Check for Traps", 2).error shouldBe null
        game.resolveStack()

        val decision = game.getPendingDecision()
        if (pick != null) {
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the caster chooses") { decision.playerId shouldBe game.player1Id }
            game.selectCards(listOf(game.findCardsInHand(2, pick).single())).error shouldBe null
            game.resolveStack()
        } else if (decision is SelectCardsDecision) {
            game.selectCards(emptyList())
            game.resolveStack()
        }
        return game
    }

    init {
        context("Check for Traps") {

            test("exiling an instant makes the opponent lose 1 life") {
                val game = castAndPick(listOf("Lightning Bolt", "Grizzly Bears"), "Lightning Bolt")
                withClue("the instant was exiled") { game.isInExile(2, "Lightning Bolt") shouldBe true }
                withClue("opponent loses 1") { game.getLifeTotal(2) shouldBe 19 }
                withClue("caster unharmed") { game.getLifeTotal(1) shouldBe 20 }
            }

            test("exiling a creature card with flash makes the opponent lose 1 life") {
                val game = castAndPick(listOf("Raging Kavu", "Grizzly Bears"), "Raging Kavu")
                withClue("the flash card was exiled") { game.isInExile(2, "Raging Kavu") shouldBe true }
                withClue("opponent loses 1") { game.getLifeTotal(2) shouldBe 19 }
                withClue("caster unharmed") { game.getLifeTotal(1) shouldBe 20 }
            }

            test("exiling any other nonland card makes the caster lose 1 life") {
                val game = castAndPick(listOf("Lightning Bolt", "Grizzly Bears"), "Grizzly Bears")
                withClue("the creature was exiled") { game.isInExile(2, "Grizzly Bears") shouldBe true }
                withClue("the instant stays in hand") { game.isInHand(2, "Lightning Bolt") shouldBe true }
                withClue("opponent unharmed") { game.getLifeTotal(2) shouldBe 20 }
                withClue("caster loses 1") { game.getLifeTotal(1) shouldBe 19 }
            }

            test("nothing exiled from an all-land hand — the caster loses 1 life") {
                val game = castAndPick(listOf("Forest"), null)
                withClue("the land stays in hand") { game.isInHand(2, "Forest") shouldBe true }
                withClue("opponent unharmed") { game.getLifeTotal(2) shouldBe 20 }
                withClue("caster loses 1") { game.getLifeTotal(1) shouldBe 19 }
            }
        }
    }
}
