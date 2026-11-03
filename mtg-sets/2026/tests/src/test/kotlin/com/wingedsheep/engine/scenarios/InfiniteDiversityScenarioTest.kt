package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Infinite Diversity (TRK #153) — {7}{R}{R} Sorcery.
 *
 * "Federation — Exile target instant or sorcery card from your graveyard. Copy that card X times,
 * where X is the number of creature types among non-Borg creatures you control. You may cast the
 * copies without paying their mana costs. Exile Infinite Diversity."
 *
 * Pins the repeat around the copy chain: X copies of the one exiled card, each its own "may",
 * the inner pass reading the outer pipeline's exiled card, and X = 0 still exiling both cards.
 */
class InfiniteDiversityScenarioTest : ScenarioTestBase() {

    init {
        fun board(vararg creatures: String): TestGame {
            var builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Infinite Diversity")
                .withLandsOnBattlefield(1, "Mountain", 9)
                .withCardInGraveyard(1, "Lightning Bolt")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            for (creature in creatures) builder = builder.withCardOnBattlefield(1, creature)
            return builder.build()
        }

        test("two creature types: two free copies of the exiled card, then it exiles itself") {
            // Grizzly Bears (Bear) + Hill Giant (Giant) = 2 creature types.
            val game = board("Grizzly Bears", "Hill Giant")
            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
            game.castSpellTargetingGraveyardCard(1, "Infinite Diversity", listOf(bolt)).error shouldBe null
            game.resolveStack()

            game.answerYesNo(true)
            game.selectTargets(listOf(game.player2Id))
            game.answerYesNo(true)
            game.selectTargets(listOf(game.player2Id))
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 14
            game.isInExile(1, "Lightning Bolt") shouldBe true
            game.isInExile(1, "Infinite Diversity") shouldBe true
            game.isInGraveyard(1, "Infinite Diversity") shouldBe false
        }

        test("each copy is its own may — declining one still offers the next") {
            val game = board("Grizzly Bears", "Hill Giant")
            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
            game.castSpellTargetingGraveyardCard(1, "Infinite Diversity", listOf(bolt)).error shouldBe null
            game.resolveStack()

            game.answerYesNo(false)
            game.answerYesNo(true)
            game.selectTargets(listOf(game.player2Id))
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 17
            game.isInExile(1, "Lightning Bolt") shouldBe true
        }

        test("no creatures: X is 0, the card is exiled but never copied") {
            val game = board()
            val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
            game.castSpellTargetingGraveyardCard(1, "Infinite Diversity", listOf(bolt)).error shouldBe null
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 20
            game.isInExile(1, "Lightning Bolt") shouldBe true
            game.isInExile(1, "Infinite Diversity") shouldBe true
        }
    }
}
