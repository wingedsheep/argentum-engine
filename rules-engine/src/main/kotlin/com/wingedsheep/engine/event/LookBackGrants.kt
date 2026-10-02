package com.wingedsheep.engine.event

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import kotlinx.serialization.Serializable

/**
 * The granted triggered abilities a permanent had immediately before it left the battlefield that
 * can't be read back off the game state afterwards — the look-back a dies / leaves-the-battlefield
 * ability needs (CR 603.10a).
 *
 * - [conditionalSelfGrantIds] — its own "has '…' as long as <condition>" grants whose condition
 *   held ([ConditionalSelfGrants]). By trigger time the permanent has no controller to evaluate the
 *   condition against.
 * - [attachmentGrantedTriggers] — the triggered abilities its Auras and Equipment granted it
 *   ("enchanted creature has 'When this creature dies, …'", [AttachmentGrantedTriggers]). By trigger
 *   time the attachment may be gone: put into the graveyard by the state-based action after its
 *   host's (CR 704.5m), destroyed in the same event, or — bestow — turned back into a creature.
 *
 * [ZoneTransitionService][com.wingedsheep.engine.handlers.effects.ZoneTransitionService] freezes
 * this onto the exit snapshot. A caller that moves several objects as one simultaneous event (a
 * board wipe, a state-based-action pass) one at a time takes it for all of them before the first
 * move ([frozen]) and hands each its own through `ZoneEntryOptions.lookBackGrants` — otherwise an
 * Aura moved before its host would already be missing from the host's look-back.
 */
@Serializable
data class LookBackGrants(
    val conditionalSelfGrantIds: List<AbilityId> = emptyList(),
    val attachmentGrantedTriggers: List<TriggeredAbility> = emptyList(),
) {
    val isEmpty: Boolean get() = conditionalSelfGrantIds.isEmpty() && attachmentGrantedTriggers.isEmpty()

    companion object {
        /** The look-back grants of [entityId] as it stands in [state]. */
        fun of(
            state: GameState,
            entityId: EntityId,
            cardRegistry: CardRegistry,
            conditionEvaluator: ConditionEvaluator,
        ): LookBackGrants = LookBackGrants(
            conditionalSelfGrantIds = ConditionalSelfGrants.activeIds(state, entityId, cardRegistry, conditionEvaluator),
            attachmentGrantedTriggers = AttachmentGrantedTriggers.of(
                state,
                state.getEntity(entityId)?.get<AttachmentsComponent>()?.attachedIds.orEmpty(),
                cardRegistry,
                conditionEvaluator,
            ),
        )

        /**
         * [of] for each of [entityIds], taken from one [state] before any of them moves — the
         * look-back for a batch that leaves as one simultaneous event. Entities with none are omitted.
         */
        fun frozen(
            state: GameState,
            entityIds: Collection<EntityId>,
            cardRegistry: CardRegistry,
            conditionEvaluator: ConditionEvaluator,
        ): Map<EntityId, LookBackGrants> = buildMap {
            for (id in entityIds) {
                val grants = of(state, id, cardRegistry, conditionEvaluator)
                if (!grants.isEmpty) put(id, grants)
            }
        }
    }
}

/**
 * The triggered abilities granted to a permanent by the Auras / Equipment attached to it — a
 * [Scope.AttachedTo] [GrantTriggeredAbility], or one wrapped in a [ConditionalStaticAbility] whose
 * condition holds with the attachment as its source ("as long as enchanted permanent is X, it has
 * '…'", Essence Leak). A face-down attachment grants nothing (CR 708.2). One entry per granting
 * attachment, so two copies of the same Aura give the ability twice.
 */
internal object AttachmentGrantedTriggers {

    fun of(
        state: GameState,
        attachmentIds: List<EntityId>,
        cardRegistry: CardRegistry,
        conditionEvaluator: ConditionEvaluator,
    ): List<TriggeredAbility> {
        if (attachmentIds.isEmpty()) return emptyList()
        val result = mutableListOf<TriggeredAbility>()
        for (attachmentId in attachmentIds) {
            val container = state.getEntity(attachmentId) ?: continue
            if (container.has<FaceDownComponent>()) continue
            val card = container.get<CardComponent>() ?: continue
            val sourceDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = container.get<ClassLevelComponent>()?.currentLevel
            for (ability in sourceDef.script.effectiveStaticAbilities(classLevel)) {
                when (ability) {
                    is GrantTriggeredAbility ->
                        if (ability.filter.scope is Scope.AttachedTo) result.add(ability.ability)

                    is ConditionalStaticAbility -> {
                        val grant = ability.ability as? GrantTriggeredAbility ?: continue
                        if (grant.filter.scope !is Scope.AttachedTo) continue
                        val controllerId = state.projectedState.getController(attachmentId) ?: continue
                        val context = EffectContext(sourceId = attachmentId, controllerId = controllerId)
                        if (conditionEvaluator.evaluate(state, ability.condition, context)) {
                            result.add(grant.ability)
                        }
                    }

                    else -> {}
                }
            }
        }
        return result
    }
}
