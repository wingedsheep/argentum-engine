package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.life.DrainLifeExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.LifeLostAmountThisTurnComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.DrainLifeEffect
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ModifyLifeLoss
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.TurnTracker
import io.kotest.matchers.shouldBe

/**
 * `DrainLife(amountPerLoser = true)`: the amount is measured for each losing player (with
 * `Player.You` meaning that player), and the gain is one event for the total actually lost.
 */
class DrainLifePerLoserAmountTest : ScenarioTestBase() {
    private val lifeLostThisTurn = DynamicAmount.TurnTracking(Player.You, TurnTracker.LIFE_LOST_AMOUNT)

    private val perLoserDrain = card("Per-Loser Drain Probe") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.DrainLife(lifeLostThisTurn, amountPerLoser = true) }
    }
    private val sharedDrain = card("Shared Drain Probe") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.DrainLife(lifeLostThisTurn) }
    }
    private val loseFiveSelf = card("Lose Five Self Probe") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.LoseLife(5, EffectTarget.Controller) }
    }
    private val loseThreeOpponent = card("Lose Three Opponent Probe") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.LoseLife(3, EffectTarget.PlayerRef(Player.EachOpponent)) }
    }
    private val doubleLoss = card("Double Loss Probe") {
        manaCost = "{0}"; typeLine = "Enchantment"
        replacementEffect(ModifyLifeLoss(multiplier = 2, appliesTo = EventPattern.LifeLossEvent(Player.EachOpponent)))
    }

    /** Player 1 has lost 5 life this turn and player 2 has lost 3, then player 1 casts [drain]. */
    private fun drainAfterLosses(drain: String, vararg onBattlefield: String): TestGame {
        val b = scenario().withPlayers()
            .withCardInHand(1, loseFiveSelf.name).withCardInHand(1, loseThreeOpponent.name).withCardInHand(1, drain)
            .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        onBattlefield.forEach { b.withCardOnBattlefield(1, it) }
        val g = b.build()
        g.castSpell(1, loseFiveSelf.name).error shouldBe null
        g.resolveStack()
        g.castSpell(1, loseThreeOpponent.name).error shouldBe null
        g.resolveStack()
        return g
    }

    init {
        listOf(perLoserDrain, sharedDrain, loseFiveSelf, loseThreeOpponent, doubleLoss)
            .forEach { cardRegistry.register(it) }

        test("the amount is measured for the losing player, not the drain's controller") {
            val g = drainAfterLosses(perLoserDrain.name)
            g.getLifeTotal(1) shouldBe 15
            g.getLifeTotal(2) shouldBe 17
            g.castSpell(1, perLoserDrain.name).error shouldBe null
            g.resolveStack()
            g.getLifeTotal(2) shouldBe 14
            g.getLifeTotal(1) shouldBe 18
        }

        test("without the flag the amount is the controller's, as before") {
            val g = drainAfterLosses(sharedDrain.name)
            g.castSpell(1, sharedDrain.name).error shouldBe null
            g.resolveStack()
            g.getLifeTotal(2) shouldBe 12
            g.getLifeTotal(1) shouldBe 20
        }

        test("the gain is one event for the life actually lost after replacement") {
            val g = drainAfterLosses(perLoserDrain.name, doubleLoss.name)
            // The doubler made the earlier 3 a 6, so player 2 has lost 6 this turn; doubled again: 12.
            g.getLifeTotal(2) shouldBe 14
            g.castSpell(1, perLoserDrain.name).error shouldBe null
            val gains = g.resolveStack().flatMap { it.events }.filterIsInstance<LifeChangedEvent>()
                .filter { it.playerId == g.player1Id && it.newLife > it.oldLife }
            g.getLifeTotal(2) shouldBe 2
            gains.size shouldBe 1
            g.getLifeTotal(1) shouldBe 27
        }

        test("in multiplayer each opponent loses their own amount, and the gain is one event for the sum") {
            val registry = CardRegistry().also { it.register(TestCards.all) }
            val config = GameConfig(
                players = (1..3).map { PlayerConfig("Player $it", Deck.of("Forest" to 40)) },
                startingPlayerIndex = 0,
                skipMulligans = true,
            )
            val init = GameInitializer(registry).initializeGame(config)
            val p = init.playerIds
            fun GameState.lost(player: EntityId, amount: Int) =
                updateEntity(player) { it.with(LifeLostAmountThisTurnComponent(amount)) }
            val state = init.state.lost(p[0], 7).lost(p[1], 3).lost(p[2], 5)

            val result = DrainLifeExecutor(PredicateEvaluator(cardRegistry = null).amounts).execute(
                state,
                DrainLifeEffect(lifeLostThisTurn, amountPerLoser = true),
                EffectContext(sourceId = null, controllerId = p[0])
            )

            fun life(player: EntityId) = result.state.getEntity(player)?.get<LifeTotalComponent>()?.life
            life(p[1]) shouldBe 17
            life(p[2]) shouldBe 15
            life(p[0]) shouldBe 28
            result.events.filterIsInstance<LifeChangedEvent>()
                .count { it.playerId == p[0] } shouldBe 1
        }

        test("a player who lost no life this turn loses nothing, and the drainer gains nothing") {
            val g = scenario().withPlayers().withCardInHand(1, perLoserDrain.name)
                .withCardInLibrary(1, "Forest").withCardInLibrary(2, "Forest")
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            g.castSpell(1, perLoserDrain.name).error shouldBe null
            g.resolveStack()
            g.getLifeTotal(2) shouldBe 20
            g.getLifeTotal(1) shouldBe 20
        }
    }
}
