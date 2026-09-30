package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * A `Self` read of a source that has left the battlefield (CR 608.2h) falls back to the departed
 * object's own battlefield-exit snapshot ([LastKnownPermanentComponent]) when no cost-paid capture
 * exists — Archfiend of the Dross's upkeep trigger resolving after it died counts the oil counters
 * it left with, not the none it has in the graveyard.
 */
class DepartedSourceLastKnownReadTest : FunSpec({

    val amounts = PredicateEvaluator(cardRegistry = null).amounts
    val player = EntityId.generate()
    val source = EntityId.generate()

    fun graveyardSource(snapshot: EntitySnapshot?): GameState {
        var container = ComponentContainer()
            .with(
                CardComponent(
                    cardDefinitionId = "Fiend",
                    name = "Fiend",
                    manaCost = ManaCost.parse("{2}{B}{B}"),
                    typeLine = TypeLine(cardTypes = setOf(CardType.CREATURE)),
                    baseStats = CreatureStats(6, 6),
                    ownerId = player
                )
            )
            .with(OwnerComponent(player))
        if (snapshot != null) container = container.with(LastKnownPermanentComponent(snapshot))
        return GameState()
            .withEntity(player, ComponentContainer())
            .withEntity(source, container)
            .addToZone(ZoneKey(player, Zone.GRAVEYARD), source)
    }

    val context = EffectContext(sourceId = source, controllerId = player)
    fun selfRead(property: EntityNumericProperty) = DynamicAmount.EntityProperty(EffectTarget.Self, property)

    test("counter and P/T reads of a departed source use its battlefield-exit snapshot") {
        val state = graveyardSource(
            EntitySnapshot(
                entityId = source,
                power = 8,
                toughness = 7,
                counters = mapOf(CounterType.OIL to 1, CounterType.PLUS_ONE_PLUS_ONE to 2)
            )
        )
        amounts.evaluate(state, selfRead(EntityNumericProperty.CounterCount(CounterType.OIL)), context) shouldBe 1
        amounts.evaluate(state, selfRead(EntityNumericProperty.CounterCount(CounterType.CHARGE)), context) shouldBe 0
        amounts.evaluate(state, selfRead(EntityNumericProperty.CounterCount(null)), context) shouldBe 3
        amounts.evaluate(state, selfRead(EntityNumericProperty.Power), context) shouldBe 8
        amounts.evaluate(state, selfRead(EntityNumericProperty.Toughness), context) shouldBe 7
    }

    test("without a snapshot the read falls through to the object as it is now") {
        val state = graveyardSource(snapshot = null)
            .updateEntity(source) { it.with(CountersComponent(mapOf(CounterType.OIL to 5))) }
        amounts.evaluate(state, selfRead(EntityNumericProperty.CounterCount(CounterType.OIL)), context) shouldBe 5
        amounts.evaluate(state, selfRead(EntityNumericProperty.Power), context) shouldBe 6
    }

    test("a reference-specific capture wins over the departed object's own snapshot") {
        val state = graveyardSource(EntitySnapshot(entityId = source, counters = mapOf(CounterType.OIL to 1)))
        val costContext = context.copy(
            lastKnownSourceSnapshot = EntitySnapshot(entityId = source, counters = mapOf(CounterType.OIL to 4))
        )
        amounts.evaluate(state, selfRead(EntityNumericProperty.CounterCount(CounterType.OIL)), costContext) shouldBe 4
    }
})
