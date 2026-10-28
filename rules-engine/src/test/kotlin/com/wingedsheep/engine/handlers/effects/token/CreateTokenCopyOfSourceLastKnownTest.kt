package com.wingedsheep.engine.handlers.effects.token

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.identity.ToxicComponent
import com.wingedsheep.engine.state.components.stack.captureEntitySnapshots
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfSourceEffect
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CreateTokenCopyOfSourceLastKnownTest : FunSpec({
    val registry = CardRegistry()
    val executor = CreateTokenCopyOfSourceExecutor(
        registry, predicateEvaluator = PredicateEvaluator(cardRegistry = registry)
    )

    listOf(false, true).forEach { returnedAsNewObject ->
        test("self copy reads ${if (returnedAsNewObject) "departure values after a blink" else "live values while the source remains"}") {
            val player = EntityId.generate()
            val source = EntityId.generate()
            val oldCard = CardComponent(
                cardDefinitionId = "Old Copy", name = "Old Copy", manaCost = ManaCost.parse("{3}{G}"),
                typeLine = TypeLine.parse("Creature — Snake"), baseStats = CreatureStats(3, 4),
                baseKeywords = setOf(Keyword.FLYING), ownerId = player,
            )
            val oldState = GameState()
                .withEntity(player, ComponentContainer())
                .withEntity(source, ComponentContainer.of(
                    oldCard, ControllerComponent(player), BattlefieldEntryTimestampComponent(1L),
                    ToxicComponent(2), CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)),
                ))
                .addToZone(ZoneKey(player, Zone.BATTLEFIELD), source)
            val departure = captureEntitySnapshots(listOf(source), oldState).single()
            departure.power shouldBe 5
            departure.toughness shouldBe 6

            val currentCard = oldCard.copy(
                cardDefinitionId = "New Copy", name = "New Copy", baseStats = CreatureStats(1, 1),
                baseKeywords = emptySet(),
            )
            // The same entity id can now represent a new battlefield visit with different copy values.
            val state = oldState.withEntity(source, ComponentContainer.of(
                currentCard, ControllerComponent(player),
                BattlefieldEntryTimestampComponent(if (returnedAsNewObject) 2L else 1L),
            ))
            val result = executor.execute(state, CreateTokenCopyOfSourceEffect(), EffectContext(
                sourceId = source, controllerId = player, sourceReferenceLost = returnedAsNewObject,
                lastKnownSourceSnapshot = departure,
            ))
            val token = result.state.getBattlefield().single {
                result.state.getEntity(it)!!.has<TokenComponent>()
            }
            val container = result.state.getEntity(token)!!
            container.get<CardComponent>() shouldBe if (returnedAsNewObject) oldCard else currentCard
            container.has<CountersComponent>() shouldBe false
            container.get<ToxicComponent>()?.amount shouldBe if (returnedAsNewObject) 2 else null
            result.state.projectedState.getPower(token) shouldBe if (returnedAsNewObject) 3 else 1
            result.state.projectedState.getToughness(token) shouldBe if (returnedAsNewObject) 4 else 1
        }
    }
})
