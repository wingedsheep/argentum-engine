package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Rafter Demon (RNA #196) — "When this creature enters, if its spectacle cost was paid, each
 * opponent discards a card." The first card to read [Conditions.SelfAlternativeCostWasPaid] as an
 * intervening "if" on an enters trigger: cast for spectacle the opponent discards, cast for its
 * mana cost nothing happens.
 */
class RafterDemonScenarioTest : ScenarioTestBase() {

    private fun game(): TestGame = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Rafter Demon")
        .withCardInHand(1, "Shock")
        .withLandsOnBattlefield(1, "Swamp", 3)
        .withLandsOnBattlefield(1, "Mountain", 3)
        .withCardInHand(2, "Grizzly Bears")
        .withCardInHand(2, "Mountain")
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Mountain")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /** Resolve the stack, answering the opponent's discard choice with the first option. */
    private fun TestGame.resolveAll() {
        var safety = 0
        while (safety++ < 20) {
            val pending = state.pendingDecision
            when {
                pending is SelectCardsDecision -> selectCards(pending.options.take(pending.minSelections))
                state.stack.isNotEmpty() -> resolveStack()
                else -> return
            }
        }
    }

    init {
        test("cast for spectacle after an opponent lost life: each opponent discards a card") {
            val game = game()
            game.castSpellTargetingPlayer(1, "Shock", 2).error shouldBe null
            game.resolveAll()
            game.getLifeTotal(2) shouldBe 18

            game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = game.state.getHand(game.player1Id).first {
                        game.state.getEntity(it)?.get<CardComponent>()?.name == "Rafter Demon"
                    },
                    useAlternativeCost = true,
                    alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE,
                )
            ).error shouldBe null
            game.resolveAll()

            game.isOnBattlefield("Rafter Demon") shouldBe true
            game.handSize(2) shouldBe 1
        }

        test("cast for its mana cost: the trigger's condition is false and nobody discards") {
            val game = game()
            game.castSpell(1, "Rafter Demon").error shouldBe null
            game.resolveAll()

            game.isOnBattlefield("Rafter Demon") shouldBe true
            game.handSize(2) shouldBe 2
        }
    }
}
