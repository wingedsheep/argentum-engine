package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.core.DrawFailedEvent
import com.wingedsheep.engine.core.StatsModifiedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.UntappedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.Sublayer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*

/**
 * Executes effects from the SDK against the game state.
 *
 * This is the bridge between the "dumb" effect data and the "smart" engine logic.
 * Each effect type maps to a specific execution function.
 */
class EffectHandler(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) {

    /**
     * Execute an effect and return the result.
     */
    fun execute(
        state: GameState,
        effect: Effect,
        context: EffectContext
    ): ExecutionResult {
        return when (effect) {
            // Life effects
            is GainLifeEffect -> executeGainLife(state, effect, context)
            is LoseLifeEffect -> executeLoseLife(state, effect, context)
            is LoseHalfLifeEffect -> executeLoseHalfLife(state, effect, context)

            // Damage effects
            is DealDamageEffect -> executeDealDamage(state, effect, context)
            is DealXDamageEffect -> executeDealXDamage(state, effect, context)
            is DealDamageToAllCreaturesEffect -> executeDealDamageToAllCreatures(state, effect, context)
            is DealDamageToAllEffect -> executeDealDamageToAll(state, effect, context)
            is DrainEffect -> executeDrain(state, effect, context)

            // Card drawing effects
            is DrawCardsEffect -> executeDrawCards(state, effect, context)
            is DiscardCardsEffect -> executeDiscardCards(state, effect, context)

            // Creature effects
            is DestroyEffect -> executeDestroy(state, effect, context)
            is DestroyAllCreaturesEffect -> executeDestroyAllCreatures(state, context)
            is DestroyAllLandsEffect -> executeDestroyAllLands(state, context)
            is ExileEffect -> executeExile(state, effect, context)
            is ReturnToHandEffect -> executeReturnToHand(state, effect, context)
            is TapUntapEffect -> executeTapUntap(state, effect, context)

            // Stat modification
            is ModifyStatsEffect -> executeModifyStats(state, effect, context)
            is AddCountersEffect -> executeAddCounters(state, effect, context)
            is RemoveCountersEffect -> executeRemoveCounters(state, effect, context)

            // Mana effects
            is AddManaEffect -> executeAddMana(state, effect, context)
            is AddColorlessManaEffect -> executeAddColorlessMana(state, effect, context)

            // Token creation
            is CreateTokenEffect -> executeCreateToken(state, effect, context)
            is CreateTreasureTokensEffect -> executeCreateTreasure(state, effect, context)

            // Library effects
            is ScryEffect -> executeScry(state, effect, context)
            is MillEffect -> executeMill(state, effect, context)
            is ShuffleLibraryEffect -> executeShuffleLibrary(state, effect, context)

            // Composite effects
            is CompositeEffect -> executeComposite(state, effect, context)

            // Conditional effects
            is ConditionalEffect -> executeConditional(state, effect, context)

            // Counter spell
            is CounterSpellEffect -> executeCounterSpell(state, context)

            // Default handler for unimplemented effects
            else -> {
                // Log unhandled effect type (in production, could emit warning event)
                ExecutionResult.success(state)
            }
        }
    }

    // =========================================================================
    // Life Effects
    // =========================================================================

    private fun executeGainLife(
        state: GameState,
        effect: GainLifeEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for life gain")

        val currentLife = state.getEntity(targetId)?.get<LifeTotalComponent>()?.life
            ?: return ExecutionResult.error(state, "Target has no life total")

        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        val newLife = currentLife + amount
        val newState = state.updateEntity(targetId) { container ->
            container.with(LifeTotalComponent(newLife))
        }

        return ExecutionResult.success(
            newState,
            listOf(LifeChangedEvent(targetId, currentLife, newLife, LifeChangeReason.LIFE_GAIN))
        )
    }

    private fun executeLoseLife(
        state: GameState,
        effect: LoseLifeEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for life loss")

        val currentLife = state.getEntity(targetId)?.get<LifeTotalComponent>()?.life
            ?: return ExecutionResult.error(state, "Target has no life total")

        val newLife = currentLife - effect.amount
        val newState = state.updateEntity(targetId) { container ->
            container.with(LifeTotalComponent(newLife))
        }

        return ExecutionResult.success(
            newState,
            listOf(LifeChangedEvent(targetId, currentLife, newLife, LifeChangeReason.LIFE_LOSS))
        )
    }

    private fun executeLoseHalfLife(
        state: GameState,
        effect: LoseHalfLifeEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for half life loss")

        val currentLife = state.getEntity(targetId)?.get<LifeTotalComponent>()?.life
            ?: return ExecutionResult.error(state, "Target has no life total")

        val lifeLoss = if (effect.roundUp) {
            (currentLife + 1) / 2
        } else {
            currentLife / 2
        }

        val newLife = currentLife - lifeLoss
        val newState = state.updateEntity(targetId) { container ->
            container.with(LifeTotalComponent(newLife))
        }

        return ExecutionResult.success(
            newState,
            listOf(LifeChangedEvent(targetId, currentLife, newLife, LifeChangeReason.LIFE_LOSS))
        )
    }

    // =========================================================================
    // Damage Effects
    // =========================================================================

    private fun executeDealDamage(
        state: GameState,
        effect: DealDamageEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for damage")

        return dealDamageToTarget(state, targetId, effect.amount, context.sourceId)
    }

    private fun executeDealXDamage(
        state: GameState,
        effect: DealXDamageEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for X damage")

        val xValue = context.xValue ?: 0
        return dealDamageToTarget(state, targetId, xValue, context.sourceId)
    }

    private fun dealDamageToTarget(
        state: GameState,
        targetId: EntityId,
        amount: Int,
        sourceId: EntityId?
    ): ExecutionResult {
        if (amount <= 0) return ExecutionResult.success(state)

        val events = mutableListOf<EngineGameEvent>()
        var newState = state

        // Check if target is a player or creature
        val lifeComponent = state.getEntity(targetId)?.get<LifeTotalComponent>()
        if (lifeComponent != null) {
            // It's a player - reduce life
            val newLife = lifeComponent.life - amount
            newState = newState.updateEntity(targetId) { container ->
                container.with(LifeTotalComponent(newLife))
            }
            events.add(LifeChangedEvent(targetId, lifeComponent.life, newLife, LifeChangeReason.DAMAGE))
        } else {
            // It's a creature - mark damage
            val currentDamage = state.getEntity(targetId)?.get<DamageComponent>()?.amount ?: 0
            newState = newState.updateEntity(targetId) { container ->
                container.with(DamageComponent(currentDamage + amount))
            }
        }

        events.add(DamageDealtEvent(sourceId, targetId, amount, false))

        return ExecutionResult.success(newState, events)
    }

    private fun executeDealDamageToAllCreatures(
        state: GameState,
        effect: DealDamageToAllCreaturesEffect,
        context: EffectContext
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            if (!cardComponent.typeLine.isCreature) continue

            // Filter by flying if specified
            val hasFlying = cardComponent.baseKeywords.any { it.name == "FLYING" }
            if (effect.onlyFlying && !hasFlying) continue
            if (effect.onlyNonFlying && hasFlying) continue

            val result = dealDamageToTarget(newState, entityId, effect.amount, context.sourceId)
            newState = result.newState
            events.addAll(result.events)
        }

        return ExecutionResult.success(newState, events)
    }

    private fun executeDealDamageToAll(
        state: GameState,
        effect: DealDamageToAllEffect,
        context: EffectContext
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        // Damage to creatures
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            if (!cardComponent.typeLine.isCreature) continue

            // Filter by flying if specified
            val hasFlying = cardComponent.baseKeywords.any { it.name == "FLYING" }
            if (effect.onlyFlyingCreatures && !hasFlying) continue
            if (effect.onlyNonFlyingCreatures && hasFlying) continue

            val result = dealDamageToTarget(newState, entityId, effect.amount, context.sourceId)
            newState = result.newState
            events.addAll(result.events)
        }

        // Damage to players
        for (playerId in state.turnOrder) {
            val result = dealDamageToTarget(newState, playerId, effect.amount, context.sourceId)
            newState = result.newState
            events.addAll(result.events)
        }

        return ExecutionResult.success(newState, events)
    }

    private fun executeDrain(
        state: GameState,
        effect: DrainEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for drain")

        // Deal damage
        val damageResult = dealDamageToTarget(state, targetId, effect.amount, context.sourceId)

        // Gain life
        val controllerId = context.controllerId
        val currentLife = damageResult.newState.getEntity(controllerId)?.get<LifeTotalComponent>()?.life ?: 0
        val newLife = currentLife + effect.amount

        val newState = damageResult.newState.updateEntity(controllerId) { container ->
            container.with(LifeTotalComponent(newLife))
        }

        val events = damageResult.events + LifeChangedEvent(
            controllerId, currentLife, newLife, LifeChangeReason.LIFE_GAIN
        )

        return ExecutionResult.success(newState, events)
    }

    // =========================================================================
    // Card Drawing Effects
    // =========================================================================

    private fun executeDrawCards(
        state: GameState,
        effect: DrawCardsEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid player for draw")

        var newState = state
        val drawnCards = mutableListOf<EntityId>()

        val libraryZone = ZoneKey(playerId, ZoneType.LIBRARY)
        val handZone = ZoneKey(playerId, ZoneType.HAND)
        val count = amountEvaluator.evaluate(state, effect.count, context)

        repeat(count) {
            val library = newState.getZone(libraryZone)
            if (library.isEmpty()) {
                // Failed to draw - game loss condition
                return ExecutionResult.success(
                    newState,
                    listOf(DrawFailedEvent(playerId, "Empty library"))
                )
            }

            // Draw from top of library (first card)
            val cardId = library.first()
            drawnCards.add(cardId)

            newState = newState.removeFromZone(libraryZone, cardId)
            newState = newState.addToZone(handZone, cardId)
        }

        return ExecutionResult.success(
            newState,
            listOf(CardsDrawnEvent(playerId, drawnCards.size, drawnCards))
        )
    }

    private fun executeDiscardCards(
        state: GameState,
        effect: DiscardCardsEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid player for discard")

        // For now, discard random cards (proper implementation needs player choice)
        var newState = state
        val discardedCards = mutableListOf<EntityId>()

        val handZone = ZoneKey(playerId, ZoneType.HAND)
        val graveyardZone = ZoneKey(playerId, ZoneType.GRAVEYARD)

        repeat(effect.count) {
            val hand = newState.getZone(handZone)
            if (hand.isEmpty()) return@repeat

            // Discard first card in hand (simplified - should be player choice)
            val cardId = hand.first()
            discardedCards.add(cardId)

            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        return ExecutionResult.success(
            newState,
            listOf(CardsDiscardedEvent(playerId, discardedCards))
        )
    }

    // =========================================================================
    // Removal Effects
    // =========================================================================

    private fun executeDestroy(
        state: GameState,
        effect: DestroyEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for destroy")

        return destroyPermanent(state, targetId)
    }

    private fun executeDestroyAllCreatures(
        state: GameState,
        context: EffectContext
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            if (!cardComponent.typeLine.isCreature) continue

            val result = destroyPermanent(newState, entityId)
            newState = result.newState
            events.addAll(result.events)
        }

        return ExecutionResult.success(newState, events)
    }

    private fun executeDestroyAllLands(
        state: GameState,
        context: EffectContext
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            if (!cardComponent.typeLine.isLand) continue

            val result = destroyPermanent(newState, entityId)
            newState = result.newState
            events.addAll(result.events)
        }

        return ExecutionResult.success(newState, events)
    }

    private fun destroyPermanent(state: GameState, entityId: EntityId): ExecutionResult {
        val container = state.getEntity(entityId)
            ?: return ExecutionResult.error(state, "Entity not found: $entityId")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card: $entityId")

        // TODO: Check for indestructible

        // Find which player's battlefield it's on
        val controllerId = container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return ExecutionResult.error(state, "Cannot determine owner")

        val ownerId = cardComponent.ownerId ?: controllerId

        // Move to graveyard
        val battlefieldZone = ZoneKey(controllerId, ZoneType.BATTLEFIELD)
        val graveyardZone = ZoneKey(ownerId, ZoneType.GRAVEYARD)

        var newState = state.removeFromZone(battlefieldZone, entityId)
        newState = newState.addToZone(graveyardZone, entityId)

        // Remove permanent-only components
        newState = newState.updateEntity(entityId) { c ->
            c.without<ControllerComponent>()
                .without<TappedComponent>()
                .without<SummoningSicknessComponent>()
                .without<DamageComponent>()
                .without<CountersComponent>()
        }

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    entityId,
                    cardComponent.name,
                    ZoneType.BATTLEFIELD,
                    ZoneType.GRAVEYARD,
                    ownerId
                )
            )
        )
    }

    private fun executeExile(
        state: GameState,
        effect: ExileEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for exile")

        val container = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Entity not found")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")

        val controllerId = container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return ExecutionResult.error(state, "Cannot determine owner")

        val ownerId = cardComponent.ownerId ?: controllerId

        // Move to exile
        val battlefieldZone = ZoneKey(controllerId, ZoneType.BATTLEFIELD)
        val exileZone = ZoneKey(ownerId, ZoneType.EXILE)

        var newState = state.removeFromZone(battlefieldZone, targetId)
        newState = newState.addToZone(exileZone, targetId)

        // Remove permanent-only components
        newState = newState.updateEntity(targetId) { c ->
            c.without<ControllerComponent>()
                .without<TappedComponent>()
                .without<SummoningSicknessComponent>()
                .without<DamageComponent>()
        }

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    targetId,
                    cardComponent.name,
                    ZoneType.BATTLEFIELD,
                    ZoneType.EXILE,
                    ownerId
                )
            )
        )
    }

    private fun executeReturnToHand(
        state: GameState,
        effect: ReturnToHandEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for return")

        val container = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Entity not found")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")

        val controllerId = container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return ExecutionResult.error(state, "Cannot determine owner")

        val ownerId = cardComponent.ownerId ?: controllerId

        // Move to owner's hand
        val battlefieldZone = ZoneKey(controllerId, ZoneType.BATTLEFIELD)
        val handZone = ZoneKey(ownerId, ZoneType.HAND)

        var newState = state.removeFromZone(battlefieldZone, targetId)
        newState = newState.addToZone(handZone, targetId)

        // Remove permanent-only components
        newState = newState.updateEntity(targetId) { c ->
            c.without<ControllerComponent>()
                .without<TappedComponent>()
                .without<SummoningSicknessComponent>()
                .without<DamageComponent>()
        }

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    targetId,
                    cardComponent.name,
                    ZoneType.BATTLEFIELD,
                    ZoneType.HAND,
                    ownerId
                )
            )
        )
    }

    // =========================================================================
    // Tap/Untap Effects
    // =========================================================================

    private fun executeTapUntap(
        state: GameState,
        effect: TapUntapEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for tap/untap")

        val cardName = state.getEntity(targetId)?.get<CardComponent>()?.name ?: "Permanent"

        val newState = state.updateEntity(targetId) { container ->
            if (effect.tap) {
                container.with(TappedComponent)
            } else {
                container.without<TappedComponent>()
            }
        }

        val event = if (effect.tap) {
            TappedEvent(targetId, cardName)
        } else {
            UntappedEvent(targetId, cardName)
        }

        return ExecutionResult.success(newState, listOf(event))
    }

    // =========================================================================
    // Stat Modification Effects
    // =========================================================================

    private fun executeModifyStats(
        state: GameState,
        effect: ModifyStatsEffect,
        context: EffectContext
    ): ExecutionResult {
        // Resolve the target creature
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for stat modification")

        // Verify target exists and is a creature
        val targetContainer = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target creature no longer exists")
        val cardComponent = targetContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Target is not a card")
        if (!cardComponent.typeLine.isCreature) {
            return ExecutionResult.error(state, "Target is not a creature")
        }

        // Create a floating effect for the stat modification
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.POWER_TOUGHNESS,
                sublayer = Sublayer.MODIFICATIONS,
                modification = SerializableModification.ModifyPowerToughness(
                    powerMod = effect.powerModifier,
                    toughnessMod = effect.toughnessModifier
                ),
                affectedEntities = setOf(targetId)
            ),
            duration = effect.duration,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
            controllerId = context.controllerId,
            timestamp = System.currentTimeMillis()
        )

        // Add the floating effect to game state
        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        // Emit event for visualization
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"
        val events = listOf(
            StatsModifiedEvent(
                targetId = targetId,
                targetName = cardComponent.name,
                powerChange = effect.powerModifier,
                toughnessChange = effect.toughnessModifier,
                sourceName = sourceName
            )
        )

        return ExecutionResult.success(newState, events)
    }

    private fun executeAddCounters(
        state: GameState,
        effect: AddCountersEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for counters")

        val counterType = try {
            CounterType.valueOf(effect.counterType.uppercase().replace(' ', '_').replace('+', 'P').replace('-', 'M').replace("/", "_"))
        } catch (e: IllegalArgumentException) {
            // Default to generic counter type if not found
            CounterType.PLUS_ONE_PLUS_ONE
        }

        val current = state.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()

        val newState = state.updateEntity(targetId) { container ->
            container.with(current.withAdded(counterType, effect.count))
        }

        return ExecutionResult.success(
            newState,
            listOf(CountersAddedEvent(targetId, effect.counterType, effect.count))
        )
    }

    private fun executeRemoveCounters(
        state: GameState,
        effect: RemoveCountersEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for counter removal")

        val counterType = try {
            CounterType.valueOf(effect.counterType.uppercase().replace(' ', '_').replace('+', 'P').replace('-', 'M').replace("/", "_"))
        } catch (e: IllegalArgumentException) {
            CounterType.PLUS_ONE_PLUS_ONE
        }

        val current = state.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()

        val newState = state.updateEntity(targetId) { container ->
            container.with(current.withRemoved(counterType, effect.count))
        }

        return ExecutionResult.success(
            newState,
            listOf(CountersRemovedEvent(targetId, effect.counterType, effect.count))
        )
    }

    // =========================================================================
    // Mana Effects
    // =========================================================================

    private fun executeAddMana(
        state: GameState,
        effect: AddManaEffect,
        context: EffectContext
    ): ExecutionResult {
        val newState = state.updateEntity(context.controllerId) { container ->
            val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            container.with(manaPool.add(effect.color, effect.amount))
        }

        return ExecutionResult.success(newState)
    }

    private fun executeAddColorlessMana(
        state: GameState,
        effect: AddColorlessManaEffect,
        context: EffectContext
    ): ExecutionResult {
        val newState = state.updateEntity(context.controllerId) { container ->
            val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            container.with(manaPool.addColorless(effect.amount))
        }

        return ExecutionResult.success(newState)
    }

    // =========================================================================
    // Token Effects
    // =========================================================================

    private fun executeCreateToken(
        state: GameState,
        effect: CreateTokenEffect,
        context: EffectContext
    ): ExecutionResult {
        var newState = state
        val createdTokens = mutableListOf<EntityId>()

        repeat(effect.count) {
            val tokenId = EntityId.generate()
            createdTokens.add(tokenId)

            // Create token entity
            val tokenName = "${effect.creatureTypes.joinToString(" ")} Token"
            val tokenComponent = CardComponent(
                cardDefinitionId = "token:$tokenName",
                name = tokenName,
                manaCost = ManaCost.ZERO,
                typeLine = TypeLine.parse("Creature - ${effect.creatureTypes.joinToString(" ")}"),
                baseStats = CreatureStats(effect.power, effect.toughness),
                baseKeywords = effect.keywords,
                colors = effect.colors,
                ownerId = context.controllerId
            )

            val container = ComponentContainer.of(
                tokenComponent,
                TokenComponent,
                ControllerComponent(context.controllerId),
                SummoningSicknessComponent
            )

            newState = newState.withEntity(tokenId, container)

            // Add to battlefield
            val battlefieldZone = ZoneKey(context.controllerId, ZoneType.BATTLEFIELD)
            newState = newState.addToZone(battlefieldZone, tokenId)
        }

        return ExecutionResult.success(newState)
    }

    private fun executeCreateTreasure(
        state: GameState,
        effect: CreateTreasureTokensEffect,
        context: EffectContext
    ): ExecutionResult {
        var newState = state

        repeat(effect.count) {
            val tokenId = EntityId.generate()

            val tokenComponent = CardComponent(
                cardDefinitionId = "token:Treasure",
                name = "Treasure",
                manaCost = ManaCost.ZERO,
                typeLine = TypeLine.parse("Artifact - Treasure"),
                ownerId = context.controllerId
            )

            val container = ComponentContainer.of(
                tokenComponent,
                TokenComponent,
                ControllerComponent(context.controllerId)
            )

            newState = newState.withEntity(tokenId, container)

            val battlefieldZone = ZoneKey(context.controllerId, ZoneType.BATTLEFIELD)
            newState = newState.addToZone(battlefieldZone, tokenId)
        }

        return ExecutionResult.success(newState)
    }

    // =========================================================================
    // Library Effects
    // =========================================================================

    private fun executeScry(
        state: GameState,
        effect: ScryEffect,
        context: EffectContext
    ): ExecutionResult {
        // Scry is a complex effect requiring player choice
        // For now, just shuffle the state timestamp
        return ExecutionResult.success(state.tick())
    }

    private fun executeMill(
        state: GameState,
        effect: MillEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid player for mill")

        var newState = state
        val milledCards = mutableListOf<EntityId>()

        val libraryZone = ZoneKey(targetId, ZoneType.LIBRARY)
        val graveyardZone = ZoneKey(targetId, ZoneType.GRAVEYARD)

        repeat(effect.count) {
            val library = newState.getZone(libraryZone)
            if (library.isEmpty()) return@repeat

            val cardId = library.first()
            milledCards.add(cardId)

            newState = newState.removeFromZone(libraryZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        return ExecutionResult.success(newState)
    }

    private fun executeShuffleLibrary(
        state: GameState,
        effect: ShuffleLibraryEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid player for shuffle")

        val libraryZone = ZoneKey(targetId, ZoneType.LIBRARY)
        val library = state.getZone(libraryZone).shuffled()

        val newZones = state.zones + (libraryZone to library)
        return ExecutionResult.success(
            state.copy(zones = newZones),
            listOf(LibraryShuffledEvent(targetId))
        )
    }

    // =========================================================================
    // Composite Effects
    // =========================================================================

    private fun executeComposite(
        state: GameState,
        effect: CompositeEffect,
        context: EffectContext
    ): ExecutionResult {
        var result = ExecutionResult.success(state)

        for (subEffect in effect.effects) {
            result = result.andThen { currentState ->
                execute(currentState, subEffect, context)
            }
            if (!result.isSuccess) break
        }

        return result
    }

    private fun executeConditional(
        state: GameState,
        effect: ConditionalEffect,
        context: EffectContext
    ): ExecutionResult {
        val conditionMet = ConditionEvaluator().evaluate(state, effect.condition, context)
        val elseEffect = effect.elseEffect

        return if (conditionMet) {
            execute(state, effect.effect, context)
        } else if (elseEffect != null) {
            execute(state, elseEffect, context)
        } else {
            ExecutionResult.success(state)
        }
    }

    private fun executeCounterSpell(
        state: GameState,
        context: EffectContext
    ): ExecutionResult {
        val targetSpell = context.targets.firstOrNull()
        if (targetSpell !is ChosenTarget.Spell) {
            return ExecutionResult.error(state, "No valid spell target")
        }

        return com.wingedsheep.engine.mechanics.stack.StackResolver()
            .counterSpell(state, targetSpell.spellEntityId)
    }

    // =========================================================================
    // Helper Functions
    // =========================================================================

    private fun resolveTarget(effectTarget: EffectTarget, context: EffectContext): EntityId? {
        return when (effectTarget) {
            is EffectTarget.Self -> context.sourceId
            is EffectTarget.Controller -> context.controllerId
            is EffectTarget.ContextTarget -> context.targets.getOrNull(effectTarget.index)?.toEntityId()
            is EffectTarget.TargetCreature,
            is EffectTarget.TargetPermanent,
            is EffectTarget.AnyTarget -> context.targets.firstOrNull()?.toEntityId()
            else -> null
        }
    }

    private fun resolvePlayerTarget(effectTarget: EffectTarget, context: EffectContext): EntityId? {
        return when (effectTarget) {
            is EffectTarget.Controller -> context.controllerId
            is EffectTarget.Opponent -> context.opponentId
            is EffectTarget.AnyPlayer -> context.targets.firstOrNull()?.toEntityId()
            else -> null
        }
    }

    private fun ChosenTarget.toEntityId(): EntityId = when (this) {
        is ChosenTarget.Player -> playerId
        is ChosenTarget.Permanent -> entityId
        is ChosenTarget.Card -> cardId
        is ChosenTarget.Spell -> spellEntityId
    }
}

/**
 * Context for effect execution.
 */
data class EffectContext(
    val sourceId: EntityId?,
    val controllerId: EntityId,
    val opponentId: EntityId?,
    val targets: List<ChosenTarget> = emptyList(),
    val xValue: Int? = null
)
