package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.LifeLostAmountThisTurnComponent
import com.wingedsheep.engine.state.components.player.LifeLostThisTurnComponent
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `LifeChangedEvent.firstThisTurn` on life *loss* — the stamp behind
 * `EventPattern.LifeLossEvent.firstTimeEachTurn` ("loses life for the first time during each of
 * their turns", Valgavoth, Harrower of Souls). It is the player's turn history: only the turn's
 * first losing event is first, whatever caused it (damage, a "loses N life" effect, a payment), and
 * a life gain in between doesn't reset it.
 */
class FirstLifeLossThisTurnTest : FunSpec({

    val predicates = PredicateEvaluator(cardRegistry = null)

    fun boot(): Pair<GameState, List<EntityId>> {
        val registry = CardRegistry().also { it.register(TestCards.all) }
        val result = GameInitializer(registry).initializeGame(
            GameConfig(
                players = (1..2).map { PlayerConfig("Player $it", Deck.of("Forest" to 40)) },
                skipMulligans = true,
            )
        )
        return result.state to result.playerIds
    }

    test("only the turn's first losing event is stamped first, across loss reasons") {
        val (state, p) = boot()

        val (afterEffect, first) = DamageUtils.loseLife(state, p[1], 2, LifeChangeReason.LIFE_LOSS, predicateEvaluator = predicates)
        first!!.firstThisTurn shouldBe true

        val (afterPayment, second) = DamageUtils.loseLife(afterEffect, p[1], 1, LifeChangeReason.PAYMENT, predicateEvaluator = predicates)
        second!!.firstThisTurn shouldBe false

        // A gain in between neither resets the loss history nor is stamped as a first loss.
        val (afterGain, _) = DamageUtils.gainLife(afterPayment, p[1], 3, predicateEvaluator = predicates)
        val (_, third) = DamageUtils.loseLife(afterGain, p[1], 1, LifeChangeReason.LIFE_LOSS, predicateEvaluator = predicates)
        third!!.firstThisTurn shouldBe false
    }

    test("another player's loss doesn't spend this player's first") {
        val (state, p) = boot()

        val (afterOther, otherEvent) = DamageUtils.loseLife(state, p[0], 2, LifeChangeReason.LIFE_LOSS, predicateEvaluator = predicates)
        otherEvent!!.firstThisTurn shouldBe true

        val (_, event) = DamageUtils.loseLife(afterOther, p[1], 2, LifeChangeReason.LIFE_LOSS, predicateEvaluator = predicates)
        event!!.firstThisTurn shouldBe true
    }

    test("once the per-turn life-lost marker is cleared, the next loss is first again") {
        val (state, p) = boot()

        val (lost, _) = DamageUtils.loseLife(state, p[1], 2, LifeChangeReason.LIFE_LOSS, predicateEvaluator = predicates)
        // The turn boundary clears the marker (CleanupPhaseManager), which is all "each turn" reads.
        val nextTurn = lost.updateEntity(p[1]) {
            it.without<LifeLostThisTurnComponent>().without<LifeLostAmountThisTurnComponent>()
        }

        val (_, event) = DamageUtils.loseLife(nextTurn, p[1], 2, LifeChangeReason.LIFE_LOSS, predicateEvaluator = predicates)
        event!!.firstThisTurn shouldBe true
    }

    test("a life gain is never stamped as a first loss by the loss history") {
        val (state, p) = boot()

        val (_, gain) = DamageUtils.gainLife(state, p[1], 2, predicateEvaluator = predicates)
        // firstThisTurn on a gain is the *gain* history (Leech Collector), unaffected by losses.
        gain!!.reason shouldBe LifeChangeReason.LIFE_GAIN
        gain.firstThisTurn shouldBe true
    }

    test("firstTimeEachTurn is a per-event axis and rejects the batch reading") {
        EventPattern.LifeLossEvent(Player.EachOpponent, firstTimeEachTurn = true).description shouldBe
            "${Player.EachOpponent.description} would lose life for the first time each turn"
        shouldThrow<IllegalArgumentException> {
            EventPattern.LifeLossEvent(Player.EachOpponent, batch = true, firstTimeEachTurn = true)
        }
    }
})
