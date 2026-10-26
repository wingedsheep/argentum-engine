package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Light Up the Stage (RNA #107) — the engine's first spectacle card. Spectacle {R} is a
 * self-alternative cost gated on "an opponent lost life this turn"; pins that the gate is closed
 * before any life loss and opens once an opponent has lost life, and that the spell then exiles
 * the top two cards of the library.
 */
class LightUpTheStageScenarioTest : ScenarioTestBase() {

    private fun game(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Light Up the Stage")
        .withCardInHand(1, "Shock")
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Lightning Bolt")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.castWithSpectacle() = execute(
        CastSpell(
            playerId = player1Id,
            cardId = state.getHand(player1Id).first {
                state.getEntity(it)?.get<CardComponent>()?.name == "Light Up the Stage"
            },
            useAlternativeCost = true,
            alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
        )
    )

    init {
        test("spectacle is unavailable while no opponent has lost life this turn") {
            val game = game()
            game.castWithSpectacle().error shouldNotBe null
        }

        test("after an opponent loses life, spectacle {R} casts it and it exiles the top two cards") {
            val game = game()
            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 18

            game.castWithSpectacle().error shouldBe null
            game.resolveStack()

            game.isInExile(1, "Grizzly Bears") shouldBe true
            game.isInExile(1, "Lightning Bolt") shouldBe true
            game.isInGraveyard(1, "Light Up the Stage") shouldBe true
        }
    }
}
