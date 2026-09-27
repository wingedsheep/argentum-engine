package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DeflectDamageSourceChoiceContinuation
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.PreventDamageFromChosenSourceContinuation
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.chosenCreatureType
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.PreventDamageEffect
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import com.wingedsheep.sdk.scripting.effects.PreventionScope
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import kotlin.reflect.KClass

/**
 * Unified executor for PreventDamageEffect.
 *
 * Dispatches to the appropriate floating effect creation based on the effect's parameters:
 * - amount-based vs prevent-all
 * - combat-only vs all-damage
 * - direction (to target, from target, both) or a recipient group
 * - source filter: any source, sources matching a filter, or one source chosen on resolution
 * - a reaction to the prevented damage (Deflecting Palm)
 */
class PreventDamageExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<PreventDamageEffect> {
    private val predicateEvaluator = amountEvaluator.predicates

    override val effectType: KClass<PreventDamageEffect> = PreventDamageEffect::class

    override fun execute(
        state: GameState,
        effect: PreventDamageEffect,
        context: EffectContext
    ): EffectResult = when (val sources = effect.sourceFilter) {
        // A chosen source needs a player decision before the shield exists.
        is PreventionSourceFilter.Chosen -> handleChosenSource(state, effect, sources.eligible, context)
        is PreventionSourceFilter.Matching -> {
            val filter = bindChosenValues(state, sources.filter, context)
            if (filter == null) {
                EffectResult.error(state, "No chosen creature type on source ${context.sourceId} for a prevention shield")
            } else {
                createFloatingEffect(state, effect, filter, context)
            }
        }
        PreventionSourceFilter.AnySource -> createFloatingEffect(state, effect, sourceFilter = null, context)
    }

    /**
     * A [PreventionSourceFilter.Matching] filter outlives the resolution that installs it, so a
     * predicate reading a choice off the ability's source — "a creature of the chosen type"
     * (Circle of Solace) — is bound to that choice now, and the shield keeps working after the
     * source leaves the battlefield. Null when the filter asks for a choice the source never made.
     */
    private fun bindChosenValues(
        state: GameState,
        filter: GameObjectFilter,
        context: EffectContext
    ): GameObjectFilter? {
        if (CardPredicate.HasChosenSubtype !in filter.cardPredicates) return filter
        val chosenType = context.sourceId?.let { state.getEntity(it) }?.chosenCreatureType() ?: return null
        return filter.copy(
            cardPredicates = filter.cardPredicates.map {
                if (it == CardPredicate.HasChosenSubtype) CardPredicate.HasSubtype(Subtype(chosenType)) else it
            }
        )
    }

    private fun handleChosenSource(
        state: GameState,
        effect: PreventDamageEffect,
        eligible: GameObjectFilter,
        context: EffectContext
    ): EffectResult {
        val controllerId = context.controllerId
        // Only sources matching the eligibility filter are offered — "an artifact source of your
        // choice" (Circle of Protection: Artifacts), "a source that shares a color with the mana
        // spent" (Protective Sphere: colored sources). Permanents are judged on projected state and
        // stack spells, which have no projection, on their base characteristics. The filter is
        // evaluated *relative to the ability's source*, so it can name something hanging off it —
        // "the card exiled with this artifact" (Mourner's Shield).
        val predicateContext = PredicateContext(controllerId = controllerId, sourceId = context.sourceId)
        val sourceIds = (state.getBattlefield() + state.stack).filter { entityId ->
            state.getEntity(entityId)?.get<CardComponent>() != null &&
                predicateEvaluator.matches(state, state.projectedState, entityId, eligible, predicateContext)
        }

        if (sourceIds.isEmpty()) return EffectResult.success(state)

        val decisionContext = DecisionContext(
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        )

        val decision = { decisionId: String -> SelectCardsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose a source of damage",
            context = decisionContext,
            options = sourceIds,
            minSelections = 1,
            maxSelections = 1,
            useTargetingUI = true
        ) }

        if (effect.onPrevented != null) {
            // Reaction path (Deflecting Palm, New Way Forward): on prevention, run an arbitrary
            // follow-up effect keyed to the prevented amount (reflect, draw, …).
            val continuation = DeflectDamageSourceChoiceContinuation(
                controllerId = controllerId,
                sourceId = context.sourceId,
            objectReferences = context.objectReferences,
                sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
                onPrevented = effect.onPrevented,
                preventDamage = effect.preventDamage
            )
            return EffectResult.from(state.suspendForDecision(decision, continuation))
        } else {
            // Prevention-only path: prevent N damage (or all, when amount is null) from chosen source
            //
            // `direction = FromTarget` with no amount means the prevention has no recipient clause at
            // all — "prevent all damage that would be dealt this turn by a source of your choice"
            // (Mourner's Shield) rather than "…dealt to you by a source of your choice" (Samite
            // Ministration). The shield then keys on the chosen source, so there is no recipient to
            // resolve and `effect.target` is irrelevant.
            val silenceChosenSource =
                effect.direction == PreventionDirection.FromTarget && effect.amount == null
            val targetId = if (silenceChosenSource) {
                controllerId
            } else {
                context.resolveTarget(effect.target)
                    ?: return EffectResult.error(state, "Could not resolve target for PreventDamageEffect with ChosenSource")
            }
            val amount = effect.amount?.let { amountEvaluator.evaluate(state, it, context) }
            if (amount != null && amount <= 0) return EffectResult.success(state)

            val continuation = PreventDamageFromChosenSourceContinuation(
                controllerId = controllerId,
                targetId = targetId,
                silenceChosenSource = silenceChosenSource,
                amount = amount,
                gainLifeFromColors = effect.gainLifeFromColors.map { it.name }.toSet(),
                sourceId = context.sourceId,
            objectReferences = context.objectReferences,
                sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
                nextInstanceOnly = effect.nextInstanceOnly,
                halvePreventedDamage = effect.halvePreventedDamage
            )
            return EffectResult.from(state.suspendForDecision(decision, continuation))
        }
    }

    /**
     * @param sourceFilter the bound [PreventionSourceFilter.Matching] filter, or null for a shield
     *   covering every source.
     */
    private fun createFloatingEffect(
        state: GameState,
        effect: PreventDamageEffect,
        sourceFilter: GameObjectFilter?,
        context: EffectContext
    ): EffectResult {
        if (effect.direction == PreventionDirection.FromTarget && effect.onPrevented != null) {
            val targetId = context.resolveTarget(effect.target)
                ?: return EffectResult.success(state)
            state.getEntity(targetId) ?: return EffectResult.success(state)
            val newState = state.installPreventAndReactShield(
                damageSourceId = targetId,
                protectedEntityId = null,
                controllerId = context.controllerId,
                effectSourceId = context.sourceId,
                effectSourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
                onPrevented = effect.onPrevented,
                preventDamage = effect.preventDamage,
                objectReferences = context.objectReferences
            )
            return EffectResult.success(newState)
        }

        // Determine affected entities
        val affectedEntities: Set<EntityId>
        val modification: SerializableModification

        when {
            // Recipient-side prevention: "prevent all damage that would be dealt to creatures you
            // control this turn", and the two shapes that include the player — "to you and creatures
            // you control" (group + flag) and "to you" alone (flag, no group; Scarecrow). No specific
            // affected entity is stored; the recipient filter is evaluated against projected state at
            // damage time with the shield controller as "you".
            effect.recipientGroup != null || effect.recipientGroupIncludesController -> {
                affectedEntities = emptySet()
                modification = SerializableModification.PreventAllDamageToGroup(
                    filter = effect.recipientGroup,
                    combatOnly = effect.scope == PreventionScope.CombatOnly,
                    includesController = effect.recipientGroupIncludesController,
                    // "… by creatures" — a Matching source filter narrows the shield to matching
                    // damage sources.
                    sourceFilter = sourceFilter
                )
            }

            // "The next time a creature of the chosen type would deal damage to you this turn,
            // prevent that damage" (Circle of Solace): a single-instance shield on the target,
            // spent by the first damage from any matching source.
            sourceFilter != null && effect.nextInstanceOnly &&
            effect.direction == PreventionDirection.ToTarget -> {
                val targetId = context.resolveTarget(effect.target)
                    ?: return EffectResult.error(state, "Could not resolve target for PreventDamageEffect")
                affectedEntities = setOf(targetId)
                modification = SerializableModification.PreventNextDamageFromMatching(sourceFilter)
            }

            // Prevent all damage — not just combat damage — that a group of sources would deal,
            // with no recipient clause ("prevent all damage that would be dealt by creatures this
            // turn", Ethereal Haze), optionally gaining the controller life for what it prevents
            // (Chant of Vitu-Ghazi). A life-gaining combat-only shield rides the same modification,
            // since only it knows how to credit the prevented amount.
            sourceFilter != null && effect.direction == PreventionDirection.FromTarget &&
            (effect.scope == PreventionScope.AllDamage || effect.gainLifeFromPrevented) -> {
                affectedEntities = emptySet()
                modification = SerializableModification.PreventAllDamageFromGroup(
                    filter = sourceFilter,
                    combatOnly = effect.scope == PreventionScope.CombatOnly,
                    controllerGainsLife = effect.gainLifeFromPrevented
                )
            }

            // Prevent combat damage from a group (e.g., non-Soldier creatures)
            sourceFilter != null && effect.direction == PreventionDirection.FromTarget -> {
                affectedEntities = emptySet()
                modification = SerializableModification.PreventCombatDamageFromGroup(sourceFilter)
            }

            // A Matching filter over one recipient has no lowering: its recipients are named by
            // `recipientGroup` / `recipientGroupIncludesController` ("to you by attacking
            // creatures"). Fail rather than guess a scope.
            sourceFilter != null -> {
                return EffectResult.error(
                    state,
                    "PreventDamageEffect with a Matching source filter needs a recipient group, " +
                        "the controller as recipient, FromTarget, or nextInstanceOnly"
                )
            }

            // Global combat damage prevention ("prevent all combat damage this turn", Fog).
            // The Effects.PreventAllCombatDamage() facade leaves `target` at its
            // EffectTarget.Controller default; an explicit target means the targeted
            // combat-only shield below.
            effect.scope == PreventionScope.CombatOnly &&
            effect.direction == PreventionDirection.ToTarget &&
            effect.amount == null &&
            effect.target == EffectTarget.Controller -> {
                affectedEntities = emptySet()
                modification = SerializableModification.PreventAllCombatDamage
            }

            // Targeted combat-only prevention ("prevent all combat damage that would be
            // dealt to it this turn", Fleeting Flight).
            effect.scope == PreventionScope.CombatOnly &&
            effect.direction == PreventionDirection.ToTarget &&
            effect.amount == null -> {
                val targetId = context.resolveTarget(effect.target)
                    ?: return EffectResult.error(state, "Could not resolve target for PreventDamageEffect")
                state.getEntity(targetId) ?: return EffectResult.success(state)
                affectedEntities = setOf(targetId)
                modification = SerializableModification.PreventAllDamageTo(combatOnly = true)
            }

            // Bidirectional combat damage prevention (to and by target)
            effect.direction == PreventionDirection.Both -> {
                val targetId = context.resolveTarget(effect.target)
                    ?: return EffectResult.success(state)
                state.getEntity(targetId) ?: return EffectResult.success(state)
                affectedEntities = setOf(targetId)
                modification = SerializableModification.PreventCombatDamageToAndBy
            }

            // Prevent all damage FROM target (silencing)
            effect.direction == PreventionDirection.FromTarget &&
            effect.amount == null -> {
                val targetId = context.resolveTarget(effect.target)
                    ?: return EffectResult.success(state)
                state.getEntity(targetId) ?: return EffectResult.success(state)
                affectedEntities = setOf(targetId)
                modification = SerializableModification.PreventAllDamageDealtBy
            }

            // Amount-based prevention (prevent next N damage to target), optionally gaining the
            // controller life for what it prevents (Candles' Glow).
            effect.amount != null -> {
                val targetId = context.resolveTarget(effect.target)
                    ?: return EffectResult.error(state, "Could not resolve target for PreventDamageEffect")
                val effectAmount = effect.amount!!
                val amount = amountEvaluator.evaluate(state, effectAmount, context)
                if (amount <= 0) return EffectResult.success(state)
                affectedEntities = setOf(targetId)
                modification = SerializableModification.PreventNextDamage(
                    amount,
                    controllerGainsLife = effect.gainLifeFromPrevented
                )
            }

            // Prevent all damage TO target.
            effect.direction == PreventionDirection.ToTarget &&
            effect.scope == PreventionScope.AllDamage -> {
                val targetId = context.resolveTarget(effect.target)
                    ?: return EffectResult.error(state, "Could not resolve target for PreventDamageEffect")
                state.getEntity(targetId) ?: return EffectResult.success(state)
                affectedEntities = setOf(targetId)
                modification = SerializableModification.PreventAllDamageTo()
            }

            else -> {
                return EffectResult.error(state, "Unsupported PreventDamageEffect configuration")
            }
        }

        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = modification,
            affectedEntities = affectedEntities,
            duration = effect.duration,
            context = context,
            timestamp = state.timestamp
        )

        return EffectResult.success(newState)
    }
}
