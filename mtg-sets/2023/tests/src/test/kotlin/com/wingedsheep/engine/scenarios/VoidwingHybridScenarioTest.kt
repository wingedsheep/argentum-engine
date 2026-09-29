package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import io.kotest.matchers.shouldBe

/**
 * Voidwing Hybrid (ONE #221) — {U}{B} 2/1 Creature — Phyrexian Bat.
 *
 * "Flying
 *  Toxic 1
 *  When you proliferate, return this card from your graveyard to your hand."
 *
 * Proof card for a "whenever you proliferate" trigger functioning from the graveyard.
 */
class VoidwingHybridScenarioTest : ScenarioTestBase() {

    private val spreading = card("Test Spreading") {
        manaCost = "{1}"
        typeLine = "Sorcery"
        oracleText = "Proliferate."
        spell { effect = Effects.Proliferate() }
    }

    private fun resolveAll(game: TestGame) {
        var guard = 0
        while ((game.state.stack.isNotEmpty() || game.hasPendingDecision()) && guard++ < 20) {
            if (game.hasPendingDecision()) game.skipSelection() else game.resolveStack()
        }
    }

    init {
        cardRegistry.register(spreading)

        test("proliferating returns it from your graveyard to your hand") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Voidwing Hybrid")
                .withCardInHand(1, "Test Spreading")
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Test Spreading").error shouldBe null
            resolveAll(game)

            game.isInHand(1, "Voidwing Hybrid") shouldBe true
            game.isInGraveyard(1, "Voidwing Hybrid") shouldBe false
        }

        test("on the battlefield it does nothing when you proliferate") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Voidwing Hybrid")
                .withCardInHand(1, "Test Spreading")
                .withLandsOnBattlefield(1, "Island", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Test Spreading").error shouldBe null
            resolveAll(game)

            game.isOnBattlefield("Voidwing Hybrid") shouldBe true
            game.isInHand(1, "Voidwing Hybrid") shouldBe false
        }

        test("an opponent's proliferate leaves it in the graveyard") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInGraveyard(1, "Voidwing Hybrid")
                .withCardInHand(2, "Test Spreading")
                .withLandsOnBattlefield(2, "Island", 1)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Test Spreading").error shouldBe null
            resolveAll(game)

            game.isInGraveyard(1, "Voidwing Hybrid") shouldBe true
        }

        test("has flying and toxic 1") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Voidwing Hybrid")
                .build()
            val bat = game.findPermanent("Voidwing Hybrid")!!
            val projected = game.state.projectedState
            projected.hasKeyword(bat, Keyword.FLYING) shouldBe true
            projected.hasKeyword(bat, "TOXIC_1") shouldBe true
        }
    }
}
