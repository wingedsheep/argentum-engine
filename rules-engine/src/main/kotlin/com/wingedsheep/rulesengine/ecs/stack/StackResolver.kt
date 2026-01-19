package com.wingedsheep.rulesengine.ecs.stack

import com.wingedsheep.rulesengine.ability.CardScriptRepository
import com.wingedsheep.rulesengine.ecs.ComponentContainer
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.ecs.event.ChosenTarget
import com.wingedsheep.rulesengine.ecs.layers.Modifier
import com.wingedsheep.rulesengine.ecs.script.EffectExecutor
import com.wingedsheep.rulesengine.ecs.script.EffectEvent
import com.wingedsheep.rulesengine.ecs.script.ResolvedTarget
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext

/**
 * Resolves items on the stack in the ECS system.
 *
 * Handles:
 * - **Permanent spells**: Move from stack to battlefield, add appropriate components
 * - **Non-permanent spells**: Execute effects, move to graveyard
 * - **Triggered abilities**: Execute effects, remove from stack
 * - **Activated abilities**: Execute effects, remove from stack
 *
 * Also handles target validation on resolution - if all targets are illegal,
 * the spell or ability "fizzles" (goes to graveyard without effect).
 */
class StackResolver(
    private val effectExecutor: EffectExecutor = EffectExecutor(),
    private val scriptRepository: CardScriptRepository? = null
) {

    // ==========================================================================
    // Resolution Results
    // ==========================================================================

    sealed interface ResolutionResult {
        /**
         * The item resolved successfully.
         *
         * @property state The game state after resolution
         * @property events Events generated during resolution
         * @property temporaryModifiers Modifiers created by the resolved spell/ability
         *   (e.g., "+3/+3 until end of turn" effects). These need to be persisted
         *   as continuous effects by the caller.
         */
        data class Resolved(
            val state: GameState,
            val events: List<StackResolutionEvent>,
            val temporaryModifiers: List<Modifier> = emptyList()
        ) : ResolutionResult

        /**
         * The spell/ability fizzled because all targets were invalid.
         */
        data class Fizzled(
            val state: GameState,
            val reason: String,
            val events: List<StackResolutionEvent>
        ) : ResolutionResult

        /**
         * The stack was empty, nothing to resolve.
         */
        data object EmptyStack : ResolutionResult

        /**
         * An error occurred during resolution.
         */
        data class Error(val message: String) : ResolutionResult
    }

    /**
     * Events generated during stack resolution.
     */
    sealed interface StackResolutionEvent {
        data class SpellResolved(val entityId: EntityId, val name: String) : StackResolutionEvent
        data class SpellFizzled(val entityId: EntityId, val name: String, val reason: String) : StackResolutionEvent
        data class PermanentEnteredBattlefield(val entityId: EntityId, val name: String, val controllerId: EntityId) : StackResolutionEvent
        data class SpellMovedToGraveyard(val entityId: EntityId, val name: String, val ownerId: EntityId) : StackResolutionEvent
        data class AbilityResolved(val description: String, val sourceId: EntityId) : StackResolutionEvent
        data class AbilityFizzled(val description: String, val sourceId: EntityId, val reason: String) : StackResolutionEvent
        data class EffectEventWrapper(val event: com.wingedsheep.rulesengine.ecs.script.EffectEvent) : StackResolutionEvent
    }

    // ==========================================================================
    // Main Resolution
    // ==========================================================================

    /**
     * Resolve the top item on the stack.
     *
     * This is the main entry point for stack resolution.
     */
    fun resolveTopOfStack(state: GameState): ResolutionResult {
        val topOfStack = state.getTopOfStack()
            ?: return ResolutionResult.EmptyStack

        val container = state.getEntity(topOfStack)
            ?: return ResolutionResult.Error("Top of stack entity not found")

        // Determine what type of item this is and resolve accordingly
        return when {
            container.has<SpellOnStackComponent>() -> resolveSpell(state, topOfStack, container)
            container.has<TriggeredAbilityOnStackComponent>() -> resolveTriggeredAbility(state, topOfStack, container)
            container.has<ActivatedAbilityOnStackComponent>() -> resolveActivatedAbility(state, topOfStack, container)
            else -> ResolutionResult.Error("Unknown stack item type")
        }
    }

    // ==========================================================================
    // Spell Resolution
    // ==========================================================================

    /**
     * Resolve a spell on the stack.
     */
    private fun resolveSpell(
        state: GameState,
        entityId: EntityId,
        container: ComponentContainer
    ): ResolutionResult {
        val spellComponent = container.get<SpellOnStackComponent>()
            ?: return ResolutionResult.Error("SpellOnStackComponent missing")
        val cardComponent = container.get<CardComponent>()
            ?: return ResolutionResult.Error("CardComponent missing")

        val events = mutableListOf<StackResolutionEvent>()

        // Check if targets are still valid (only if spell has targets)
        if (spellComponent.hasTargets) {
            val validTargets = validateTargets(state, spellComponent.targets)
            if (validTargets.isEmpty()) {
                // All targets invalid - spell fizzles
                return fizzleSpell(state, entityId, cardComponent, "All targets are invalid")
            }
        }

        // Remove from stack first (popFromStack returns Pair<EntityId?, GameState>)
        val (_, stateAfterPop) = state.popFromStack()
        var newState = stateAfterPop

        // Resolve based on spell type
        val definition = cardComponent.definition
        val isPermanent = definition.isPermanent

        val temporaryModifiers = mutableListOf<Modifier>()

        if (isPermanent) {
            // Permanent spell - move to battlefield
            newState = resolvePermanentSpell(newState, entityId, container, spellComponent, cardComponent)
            events.add(StackResolutionEvent.SpellResolved(entityId, cardComponent.name))
            events.add(
                StackResolutionEvent.PermanentEnteredBattlefield(
                    entityId,
                    cardComponent.name,
                    spellComponent.casterId
                )
            )
        } else {
            // Non-permanent spell (instant/sorcery) - execute effects and move to graveyard
            val effectResult = resolveNonPermanentSpell(newState, entityId, spellComponent, cardComponent)
            newState = effectResult.state
            events.addAll(effectResult.events)
            temporaryModifiers.addAll(effectResult.temporaryModifiers)
            events.add(StackResolutionEvent.SpellResolved(entityId, cardComponent.name))
        }

        return ResolutionResult.Resolved(newState, events, temporaryModifiers)
    }

    /**
     * Resolve a permanent spell by moving it to the battlefield.
     */
    private fun resolvePermanentSpell(
        state: GameState,
        entityId: EntityId,
        container: ComponentContainer,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent
    ): GameState {
        val controllerId = spellComponent.casterId

        // Remove the SpellOnStackComponent and add permanent components
        var newContainer = container.without<SpellOnStackComponent>()

        // Add controller component
        newContainer = newContainer.with(ControllerComponent(controllerId))

        // Creatures enter with summoning sickness
        if (cardComponent.isCreature) {
            newContainer = newContainer.with(SummoningSicknessComponent)
        }

        // Handle auras - they need to attach to their target
        if (cardComponent.isAura && spellComponent.targets.isNotEmpty()) {
            val target = spellComponent.targets.first()
            val targetId = when (target) {
                is ChosenTarget.Permanent -> target.entityId
                is ChosenTarget.Player -> target.playerId
                is ChosenTarget.Card -> target.cardId
            }
            newContainer = newContainer.with(AttachedToComponent(targetId))
        }

        // Update the entity and add to battlefield
        return state
            .updateEntity(entityId) { newContainer }
            .addToZone(entityId, ZoneId.BATTLEFIELD)
    }

    /**
     * Result of resolving a non-permanent spell.
     */
    private data class SpellResolutionResult(
        val state: GameState,
        val events: List<StackResolutionEvent>,
        val temporaryModifiers: List<Modifier>
    )

    /**
     * Resolve a non-permanent spell by executing its effects.
     */
    private fun resolveNonPermanentSpell(
        state: GameState,
        entityId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent
    ): SpellResolutionResult {
        val events = mutableListOf<StackResolutionEvent>()
        val modifiers = mutableListOf<Modifier>()

        var newState = state

        // Execute the spell's effects (look up from script repository)
        val script = scriptRepository?.getScript(cardComponent.name)
        val spellEffect = script?.spellEffect

        if (spellEffect != null) {
            val context = ExecutionContext(
                controllerId = spellComponent.casterId,
                sourceId = entityId,
                targets = spellComponent.targets.map { chosenToResolvedTarget(it) }
            )

            val result = effectExecutor.execute(newState, spellEffect.effect, context)
            newState = result.state
            events.addAll(result.events.map { StackResolutionEvent.EffectEventWrapper(it) })
            modifiers.addAll(result.temporaryModifiers)
        }

        // Move to graveyard
        val ownerId = cardComponent.ownerId
        val graveyardZone = ZoneId.graveyard(ownerId)

        // Remove SpellOnStackComponent before going to graveyard
        newState = newState.updateEntity(entityId) { c ->
            c.without<SpellOnStackComponent>()
        }

        newState = newState.addToZone(entityId, graveyardZone)
        events.add(StackResolutionEvent.SpellMovedToGraveyard(entityId, cardComponent.name, ownerId))

        return SpellResolutionResult(newState, events, modifiers)
    }

    /**
     * Fizzle a spell (all targets became invalid).
     */
    private fun fizzleSpell(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        reason: String
    ): ResolutionResult {
        val events = mutableListOf<StackResolutionEvent>()

        // Remove from stack
        val (_, stateAfterPop) = state.popFromStack()
        var newState = stateAfterPop

        // Move to graveyard (spells always go to owner's graveyard when fizzling)
        val ownerId = cardComponent.ownerId
        val graveyardZone = ZoneId.graveyard(ownerId)

        // Remove SpellOnStackComponent
        newState = newState.updateEntity(entityId) { c ->
            c.without<SpellOnStackComponent>()
        }

        newState = newState.addToZone(entityId, graveyardZone)

        events.add(StackResolutionEvent.SpellFizzled(entityId, cardComponent.name, reason))
        events.add(StackResolutionEvent.SpellMovedToGraveyard(entityId, cardComponent.name, ownerId))

        return ResolutionResult.Fizzled(newState, reason, events)
    }

    // ==========================================================================
    // Triggered Ability Resolution
    // ==========================================================================

    /**
     * Resolve a triggered ability on the stack.
     */
    private fun resolveTriggeredAbility(
        state: GameState,
        entityId: EntityId,
        container: ComponentContainer
    ): ResolutionResult {
        val triggerComponent = container.get<TriggeredAbilityOnStackComponent>()
            ?: return ResolutionResult.Error("TriggeredAbilityOnStackComponent missing")

        val events = mutableListOf<StackResolutionEvent>()
        val temporaryModifiers = mutableListOf<Modifier>()

        // Check if targets are still valid
        if (triggerComponent.hasTargets) {
            val validTargets = validateTargets(state, triggerComponent.targets)
            if (validTargets.isEmpty()) {
                // All targets invalid - ability fizzles
                return fizzleTriggeredAbility(state, entityId, triggerComponent, "All targets are invalid")
            }
        }

        // Remove from stack
        val (_, stateAfterPop) = state.popFromStack()
        var newState = stateAfterPop

        // Execute the ability's effects
        val context = ExecutionContext(
            controllerId = triggerComponent.controllerId,
            sourceId = triggerComponent.sourceId,
            targets = triggerComponent.targets.map { chosenToResolvedTarget(it) }
        )

        val ability = triggerComponent.ability
        val result = effectExecutor.execute(newState, ability.effect, context)
        newState = result.state
        events.addAll(result.events.map { StackResolutionEvent.EffectEventWrapper(it) })
        temporaryModifiers.addAll(result.temporaryModifiers)
        events.add(StackResolutionEvent.AbilityResolved(triggerComponent.description, triggerComponent.sourceId))

        // Remove the triggered ability entity (it only exists on the stack)
        newState = newState.removeEntity(entityId)

        return ResolutionResult.Resolved(newState, events, temporaryModifiers)
    }

    /**
     * Fizzle a triggered ability.
     */
    private fun fizzleTriggeredAbility(
        state: GameState,
        entityId: EntityId,
        triggerComponent: TriggeredAbilityOnStackComponent,
        reason: String
    ): ResolutionResult {
        val events = mutableListOf<StackResolutionEvent>()

        // Remove from stack
        val (_, stateAfterPop) = state.popFromStack()
        var newState = stateAfterPop

        // Remove the entity
        newState = newState.removeEntity(entityId)

        events.add(StackResolutionEvent.AbilityFizzled(triggerComponent.description, triggerComponent.sourceId, reason))

        return ResolutionResult.Fizzled(newState, reason, events)
    }

    // ==========================================================================
    // Activated Ability Resolution
    // ==========================================================================

    /**
     * Resolve an activated ability on the stack.
     */
    private fun resolveActivatedAbility(
        state: GameState,
        entityId: EntityId,
        container: ComponentContainer
    ): ResolutionResult {
        val abilityComponent = container.get<ActivatedAbilityOnStackComponent>()
            ?: return ResolutionResult.Error("ActivatedAbilityOnStackComponent missing")

        val events = mutableListOf<StackResolutionEvent>()
        val temporaryModifiers = mutableListOf<Modifier>()

        // Check if targets are still valid
        if (abilityComponent.hasTargets) {
            val validTargets = validateTargets(state, abilityComponent.targets)
            if (validTargets.isEmpty()) {
                // All targets invalid - ability fizzles
                return fizzleActivatedAbility(state, entityId, abilityComponent, "All targets are invalid")
            }
        }

        // Remove from stack
        val (_, stateAfterPop) = state.popFromStack()
        var newState = stateAfterPop

        // Execute the ability's effect
        val context = ExecutionContext(
            controllerId = abilityComponent.controllerId,
            sourceId = abilityComponent.sourceId,
            targets = abilityComponent.targets.map { chosenToResolvedTarget(it) }
        )

        val result = effectExecutor.execute(newState, abilityComponent.effect, context)
        newState = result.state
        events.addAll(result.events.map { StackResolutionEvent.EffectEventWrapper(it) })
        temporaryModifiers.addAll(result.temporaryModifiers)
        events.add(StackResolutionEvent.AbilityResolved(abilityComponent.sourceName, abilityComponent.sourceId))

        // Remove the activated ability entity
        newState = newState.removeEntity(entityId)

        return ResolutionResult.Resolved(newState, events, temporaryModifiers)
    }

    /**
     * Fizzle an activated ability.
     */
    private fun fizzleActivatedAbility(
        state: GameState,
        entityId: EntityId,
        abilityComponent: ActivatedAbilityOnStackComponent,
        reason: String
    ): ResolutionResult {
        val events = mutableListOf<StackResolutionEvent>()

        val (_, stateAfterPop) = state.popFromStack()
        var newState = stateAfterPop
        newState = newState.removeEntity(entityId)

        events.add(StackResolutionEvent.AbilityFizzled(abilityComponent.sourceName, abilityComponent.sourceId, reason))

        return ResolutionResult.Fizzled(newState, reason, events)
    }

    // ==========================================================================
    // Target Validation
    // ==========================================================================

    /**
     * Validate targets and return only the valid ones.
     *
     * A target is valid if:
     * - For players: The player exists and hasn't lost the game
     * - For permanents: The permanent is still on the battlefield
     * - For cards: The card is still in the expected zone
     */
    private fun validateTargets(
        state: GameState,
        targets: List<ChosenTarget>
    ): List<ChosenTarget> {
        return targets.filter { target ->
            when (target) {
                is ChosenTarget.Player -> {
                    // Player target is valid if they haven't lost
                    val container = state.getEntity(target.playerId)
                    container != null &&
                            container.has<PlayerComponent>() &&
                            !container.has<LostGameComponent>()
                }
                is ChosenTarget.Permanent -> {
                    // Permanent target is valid if still on battlefield
                    target.entityId in state.getBattlefield()
                }
                is ChosenTarget.Card -> {
                    // Card target is valid if still in the specified zone
                    target.cardId in state.getZone(target.zoneId)
                }
            }
        }
    }

    // ==========================================================================
    // Utility Methods
    // ==========================================================================

    /**
     * Convert ChosenTarget to ResolvedTarget for effect execution.
     */
    private fun chosenToResolvedTarget(chosen: ChosenTarget): ResolvedTarget {
        return when (chosen) {
            is ChosenTarget.Player -> ResolvedTarget.Player(chosen.playerId)
            is ChosenTarget.Permanent -> ResolvedTarget.Permanent(chosen.entityId)
            is ChosenTarget.Card -> ResolvedTarget.Permanent(chosen.cardId) // Treat cards as permanents for now
        }
    }
}
