package com.wingedsheep.engine.handlers.effects.life

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.CantGainLifeComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.effects.ExchangeLifeTotalsEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ExchangeLifeTotalsTest : FunSpec({
    val registry = CardRegistry().also { it.register(TestCards.all) }
    val executor = ExchangeLifeTotalsExecutor(registry, PredicateEvaluator(cardRegistry = registry))
    fun boot(): Pair<GameState, List<EntityId>> {
        val result = GameInitializer(registry).initializeGame(GameConfig(
            players = listOf(PlayerConfig("A", Deck.of("Forest" to 40)), PlayerConfig("B", Deck.of("Forest" to 40))),
            startingPlayerIndex = 0, skipMulligans = true,
        ))
        return result.state to result.playerIds
    }
    fun GameState.replacement(controller: EntityId, replacement: ReplacementEffect): GameState {
        val id = EntityId.generate()
        return withEntity(id, ComponentContainer.of(
            ControllerComponent(controller), ReplacementEffectSourceComponent(listOf(replacement)),
        )).addToZone(ZoneKey(controller, Zone.BATTLEFIELD), id)
    }
    fun exchange(state: GameState, p: List<EntityId>, draw: Boolean = false, target: EntityId = p[1]) =
        executor.execute(state, ExchangeLifeTotalsEffect(EffectTarget.ContextTarget(0), draw),
            EffectContext(sourceId = null, controllerId = p[0], targets = listOf(ChosenTarget.Player(target))))

    for (controllerLower in listOf(true, false)) {
        test("gain prohibition cancels both halves; controller lower = $controllerLower") {
            val (base, p) = boot()
            val low = p[if (controllerLower) 0 else 1]
            val high = p[if (controllerLower) 1 else 0]
            val state = base.withLifeTotal(low, 15).withLifeTotal(high, 20)
                .updateEntity(low) { it.with(CantGainLifeComponent()) }
            val result = exchange(state, p, draw = true)
            result.state shouldBe state
            result.events shouldBe emptyList()
        }
        test("loss prohibition cancels both halves; controller lower = $controllerLower") {
            val (base, p) = boot()
            val low = p[if (controllerLower) 0 else 1]
            val high = p[if (controllerLower) 1 else 0]
            val state = base.withLifeTotal(low, 15).withLifeTotal(high, 20).replacement(high,
                ModifyLifeLoss(multiplier = 0, appliesTo = EventPattern.LifeLossEvent(Player.You)))
            val result = exchange(state, p, draw = true)
            result.state shouldBe state
            result.events shouldBe emptyList()
        }
    }
    test("gain lock on the losing player and loss lock on the gaining player allow exchange") {
        val (base, p) = boot()
        val state = base.withLifeTotal(p[0], 15).withLifeTotal(p[1], 20)
            .updateEntity(p[1]) { it.with(CantGainLifeComponent()) }
            .replacement(p[0], ModifyLifeLoss(multiplier = 0, appliesTo = EventPattern.LifeLossEvent(Player.You)))
        val result = exchange(state, p)
        result.state.lifeTotal(p[0]) shouldBe 20
        result.state.lifeTotal(p[1]) shouldBe 15
        result.events.filterIsInstance<LifeChangedEvent>().map { it.reason } shouldBe
            listOf(LifeChangeReason.LIFE_GAIN, LifeChangeReason.LIFE_LOSS)
    }
    test("inactive conditional loss lock does not cancel exchange") {
        val (base, p) = boot()
        val state = base.withLifeTotal(p[0], 15).replacement(p[1], ModifyLifeLoss(
            multiplier = 0, appliesTo = EventPattern.LifeLossEvent(Player.You),
            restrictions = listOf(Conditions.LifeAtMost(10)),
        ))
        exchange(state, p).state.lifeTotal(p[1]) shouldBe 15
    }
    test("gain and loss replacements modify a permitted exchange") {
        val (base, p) = boot()
        val state = base.withLifeTotal(p[0], 15)
            .replacement(p[0], ModifyLifeGain(multiplier = 2, appliesTo = EventPattern.LifeGainEvent(Player.You)))
            .replacement(p[1], ModifyLifeLoss(multiplier = 2, appliesTo = EventPattern.LifeLossEvent(Player.You)))
        val result = exchange(state, p)
        result.state.lifeTotal(p[0]) shouldBe 25
        result.state.lifeTotal(p[1]) shouldBe 10
    }
    test("a flat replacement reducing this loss to zero does not prohibit the exchange") {
        val (base, p) = boot()
        val state = base.withLifeTotal(p[0], 15).replacement(p[1], ModifyLifeLoss(
            multiplier = 1, modifier = -5, appliesTo = EventPattern.LifeLossEvent(Player.You)))
        val result = exchange(state, p)
        result.state.lifeTotal(p[0]) shouldBe 20
        result.state.lifeTotal(p[1]) shouldBe 20
    }
    test("equal totals and self exchange emit no events or draws") {
        val (state, p) = boot()
        exchange(state, p, draw = true).state shouldBe state
        exchange(state, p, draw = true).events shouldBe emptyList()
        val unequal = state.withLifeTotal(p[0], 15)
        exchange(unequal, p, draw = true, target = p[0]).state shouldBe unequal
        exchange(unequal, p, draw = true, target = p[0]).events shouldBe emptyList()
    }
    test("draws use actual modified life lost by the controller") {
        val (base, p) = boot()
        val state = base.withLifeTotal(p[1], 15).replacement(p[0], ModifyLifeLoss(
            multiplier = 2, appliesTo = EventPattern.LifeLossEvent(Player.You)))
        val result = exchange(state, p, draw = true)
        result.state.lifeTotal(p[0]) shouldBe 10
        result.state.getHand(p[0]).size shouldBe state.getHand(p[0]).size + 10
    }
    test("both replacement conditions see the life totals before the simultaneous exchange") {
        val (base, p) = boot()
        val state = base.withLifeTotal(p[0], 15).replacement(p[0], ModifyLifeLoss(
            multiplier = 2, appliesTo = EventPattern.LifeLossEvent(Player.EachOpponent),
            restrictions = listOf(Conditions.LifeAtMost(15)),
        ))
        val result = exchange(state, p)
        result.state.lifeTotal(p[0]) shouldBe 20
        result.state.lifeTotal(p[1]) shouldBe 10
    }
    test("negative totals use their actual values") {
        val (base, p) = boot()
        val result = exchange(base.withLifeTotal(p[0], -2).withLifeTotal(p[1], 3), p)
        result.state.lifeTotal(p[0]) shouldBe 3
        result.state.lifeTotal(p[1]) shouldBe -2
    }

})
