package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.MaximumHandSize
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.player.PlayerEffectRemoval
import com.wingedsheep.engine.state.components.player.PlayerNoMaximumHandSizeComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.RemoveMaximumHandSizeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "You have no maximum hand size until your next turn" (Deep Space Scan, Live Long and Prosper):
 * [RemoveMaximumHandSizeEffect] with [Duration.UntilYourNextTurn]. The limit is lifted through the
 * rest of this turn (so its cleanup discards nothing) and every other player's turn, and returns
 * once the effect's controller begins their next turn — after that turn's untap step, the same
 * hook as every other "until your next turn" effect. The rest-of-game default is unchanged.
 */
class TemporaryNoMaximumHandSizeTest : FunSpec({

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Forest" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.max(playerId: EntityId): Int? = MaximumHandSize.effective(
        state, playerId, cardRegistry, services.conditionEvaluator, services.conditionEvaluator.amounts
    )

    fun GameTestDriver.apply(controller: EntityId, target: EntityId, duration: Duration) {
        val result = services.effectExecutorRegistry.execute(
            state,
            Effects.RemoveMaximumHandSize(EffectTarget.ContextTarget(0), duration),
            EffectContext(sourceId = null, controllerId = controller, targets = listOf(ChosenTarget.Player(target)))
        )
        result.error shouldBe null
        replaceState(result.state)
    }

    test("lasts through this turn and the opponent's turn, and ends on your next turn") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.apply(me, me, Duration.UntilYourNextTurn)
        d.max(me) shouldBe null

        repeat(4) { d.putCardInHand(me, "Forest") }
        val handBefore = d.getHandSize(me)
        d.passPriorityUntil(Step.UPKEEP, opp)
        withClue("no discard to hand size at this turn's cleanup") { d.getHandSize(me) shouldBe handBefore }
        d.max(me) shouldBe null

        d.passPriorityUntil(Step.UPKEEP, me)
        d.max(me) shouldBe 7
        d.state.getEntity(me)?.has<PlayerNoMaximumHandSizeComponent>() shouldBe false
    }

    test("keyed to the controller's next turn, not the affected player's") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.apply(me, opp, Duration.UntilYourNextTurn)
        d.max(opp) shouldBe null
        d.max(me) shouldBe 7

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        withClue("the opponent's own turn doesn't end it") { d.max(opp) shouldBe null }
        d.passPriorityUntil(Step.UPKEEP, me)
        d.max(opp) shouldBe 7
    }

    test("a rest-of-game grant isn't downgraded by a later temporary one") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.apply(me, me, Duration.Permanent)
        d.apply(me, me, Duration.UntilYourNextTurn)
        d.state.getEntity(me)?.get<PlayerNoMaximumHandSizeComponent>()?.removeOn shouldBe PlayerEffectRemoval.Permanent
        d.passPriorityUntil(Step.UPKEEP, opp)
        d.passPriorityUntil(Step.UPKEEP, me)
        d.max(me) shouldBe null
    }

    test("a rest-of-game grant replaces a temporary one") {
        val d = driver(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.apply(me, me, Duration.UntilYourNextTurn)
        d.apply(me, me, Duration.Permanent)
        d.passPriorityUntil(Step.UPKEEP, opp)
        d.passPriorityUntil(Step.UPKEEP, me)
        d.max(me) shouldBe null
    }

    test("the default duration is still the rest of the game") {
        Effects.RemoveMaximumHandSize() shouldBe RemoveMaximumHandSizeEffect(EffectTarget.Controller, Duration.Permanent)
        Effects.RemoveMaximumHandSize().description.endsWith("for the rest of the game") shouldBe true
        Effects.RemoveMaximumHandSize(duration = Duration.UntilYourNextTurn).description.endsWith("until your next turn") shouldBe true
    }

    test("only rest-of-game and until-your-next-turn durations are accepted") {
        shouldThrow<IllegalArgumentException> { RemoveMaximumHandSizeEffect(duration = Duration.EndOfTurn) }
    }
})
