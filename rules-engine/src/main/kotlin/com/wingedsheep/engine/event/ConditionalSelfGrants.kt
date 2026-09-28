package com.wingedsheep.engine.event

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.Scope

/**
 * "This creature has '<triggered ability>' as long as <condition>" — a [Scope.Self]
 * [GrantTriggeredAbility] wrapped in a [ConditionalStaticAbility] on the permanent's own definition.
 *
 * Live, the condition is evaluated with the permanent as its source each time triggers are computed
 * ([TriggerAbilityResolver]). A leaves-the-battlefield ability can't be read that way: CR 603.10a
 * looks back in time, so whether the permanent *had* the granted "when this creature dies" is
 * decided by the game state immediately before the event — by trigger time the permanent is gone
 * and has no controller to evaluate against, and anything that left alongside it (Oculus Whelp's
 * transformed permanent dying in the same wipe) has left too. [ZoneTransitionService] therefore
 * freezes [activeIds] onto the exit snapshot — taken before the whole batch moves when several
 * objects leave as one event ([frozen], carried in as `ZoneEntryOptions.conditionalSelfGrantIds`) —
 * and the dies / leaves detectors read the abilities back with [byIds].
 */
internal object ConditionalSelfGrants {

    /** The conditional self-granted triggered abilities of [entityId] whose condition holds in [state]. */
    fun active(
        state: GameState,
        entityId: com.wingedsheep.sdk.model.EntityId,
        cardRegistry: CardRegistry,
        conditionEvaluator: ConditionEvaluator,
    ): List<TriggeredAbility> {
        val container = state.getEntity(entityId) ?: return emptyList()
        if (container.has<FaceDownComponent>()) return emptyList()
        val card = container.get<CardComponent>() ?: return emptyList()
        val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: return emptyList()
        val classLevel = container.get<ClassLevelComponent>()?.currentLevel
        val controllerId = state.projectedState.getController(entityId) ?: return emptyList()
        val context = EffectContext(sourceId = entityId, controllerId = controllerId)
        return cardDef.script.effectiveStaticAbilities(classLevel).mapNotNull { ability ->
            val conditional = ability as? ConditionalStaticAbility ?: return@mapNotNull null
            val grant = conditional.ability as? GrantTriggeredAbility ?: return@mapNotNull null
            if (grant.filter.scope !is Scope.Self) return@mapNotNull null
            grant.ability.takeIf { conditionEvaluator.evaluate(state, conditional.condition, context) }
        }
    }

    /** [active], reduced to the ability ids a snapshot can carry. */
    fun activeIds(
        state: GameState,
        entityId: com.wingedsheep.sdk.model.EntityId,
        cardRegistry: CardRegistry,
        conditionEvaluator: ConditionEvaluator,
    ): List<AbilityId> = active(state, entityId, cardRegistry, conditionEvaluator).map { it.id }

    /**
     * [activeIds] for each of [entityIds], taken from one [state] before any of them moves — the
     * look-back for a batch that leaves as one simultaneous event. Entities with none are omitted.
     */
    fun frozen(
        state: GameState,
        entityIds: Collection<com.wingedsheep.sdk.model.EntityId>,
        cardRegistry: CardRegistry,
        conditionEvaluator: ConditionEvaluator,
    ): Map<com.wingedsheep.sdk.model.EntityId, List<AbilityId>> = buildMap {
        for (id in entityIds) {
            val ids = activeIds(state, id, cardRegistry, conditionEvaluator)
            if (ids.isNotEmpty()) put(id, ids)
        }
    }

    /** The conditional self-granted triggered abilities of [cardDefinitionId] named by [ids]. */
    fun byIds(cardDefinitionId: String, ids: List<AbilityId>, cardRegistry: CardRegistry): List<TriggeredAbility> {
        if (ids.isEmpty()) return emptyList()
        val cardDef = cardRegistry.getCard(cardDefinitionId) ?: return emptyList()
        // Every class level: the frozen ids were already filtered by the level the permanent had.
        return cardDef.script.effectiveStaticAbilities(Int.MAX_VALUE).mapNotNull { ability ->
            val grant = (ability as? ConditionalStaticAbility)?.ability as? GrantTriggeredAbility
            grant?.ability?.takeIf { it.id in ids }
        }
    }
}
