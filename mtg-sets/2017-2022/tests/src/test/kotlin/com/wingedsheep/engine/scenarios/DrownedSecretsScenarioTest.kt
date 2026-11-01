package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe

/**
 * Drowned Secrets (GRN #39) — {1}{U} Enchantment.
 *
 *   Whenever you cast a blue spell, target player mills two cards.
 */
class DrownedSecretsScenarioTest : ScenarioTestBase() {

    init {
        val blueSpell = card("Test Blue Ritual") {
            manaCost = "{U}"
            typeLine = "Instant"
            spell { effect = Effects.GainLife(1) }
        }
        val redSpell = card("Test Red Ritual") {
            manaCost = "{R}"
            typeLine = "Instant"
            spell { effect = Effects.GainLife(1) }
        }
        cardRegistry.register(listOf(blueSpell, redSpell))

        test("casting a blue spell makes the target player mill two") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Drowned Secrets")
                .withCardInHand(1, "Test Blue Ritual")
                .withLandsOnBattlefield(1, "Island", 1)
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Hill Giant")
                .withCardInLibrary(2, "Craw Wurm")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Test Blue Ritual").error shouldBe null
            if (game.hasPendingDecision()) game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            game.graveyardSize(2) shouldBe 2
            game.librarySize(2) shouldBe 1
            game.getLifeTotal(1) shouldBe 21
        }

        test("casting a non-blue spell does not trigger it") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Drowned Secrets")
                .withCardInHand(1, "Test Red Ritual")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(2, "Grizzly Bears")
                .withCardInLibrary(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Test Red Ritual").error shouldBe null
            game.hasPendingDecision() shouldBe false
            game.resolveStack()

            game.graveyardSize(2) shouldBe 0
            game.librarySize(2) shouldBe 2
        }
    }
}
