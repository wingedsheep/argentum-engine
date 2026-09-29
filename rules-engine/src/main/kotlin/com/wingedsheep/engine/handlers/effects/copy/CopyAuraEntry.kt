package com.wingedsheep.engine.handlers.effects.copy

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.targeting.PlayerProtectionRules
import com.wingedsheep.engine.handlers.predicates.EnchantRestriction
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect

/** Attachment choices use the resulting identity without installing it in the old zone. */
object CopyAuraEntry {
    data class Preparation(val context: EffectContext, val pause: EffectResult? = null)

    fun legalHosts(
        state: GameState, id: EntityId, card: CardComponent, controller: EntityId,
        registry: CardRegistry, finder: TargetFinder, evaluator: PredicateEvaluator,
    ): List<EntityId> {
        if (!card.isAura || card.typeLine.isCreature) return emptyList()
        val requirement = registry.getCard(card.cardDefinitionId)?.script?.auraTarget ?: return emptyList()
        val preview = state.updateEntity(id) { it.with(card).with(ControllerComponent(controller)) }
        val projected = preview.projectedState
        return finder.findLegalTargets(preview, requirement, controller, id, ignoreTargetingRestrictions = true)
            .filter { host -> host != id &&
                !projected.hasKeyword(host, com.wingedsheep.sdk.core.AbilityFlag.CANT_BE_ENCHANTED) &&
                !(if (host in state.turnOrder) PlayerProtectionRules.isProtectedFromSource(
                    preview, host, id, controller, evaluator)
                else EnchantRestriction.hostProtectedFromAttachment(preview, projected, registry, id, card, host)) }
    }

    fun question(id: EntityId, name: String, controller: EntityId, hosts: List<EntityId>): (String) -> ChooseTargetsDecision =
        { decisionId -> ChooseTargetsDecision(
            id = decisionId, playerId = controller, prompt = "Choose what $name enchants",
            context = DecisionContext(sourceId = id, sourceName = name, phase = DecisionPhase.RESOLUTION),
            targetRequirements = listOf(TargetRequirementInfo(0, "what $name enchants", 1, 1)),
            legalTargets = mapOf(0 to hosts),
        ) }

    fun prepare(
        state: GameState, effect: Effect, context: EffectContext, entrants: Map<EntityId, EntityId>,
        registry: CardRegistry, finder: TargetFinder, evaluator: PredicateEvaluator,
    ): Preparation {
        if (context.entryCopies.isEmpty()) return Preparation(context)
        var prepared = context
        val order = state.apnapOrder.withIndex().associate { it.value to it.index }
        for ((id, controller) in entrants.entries.sortedBy { order[it.value] ?: Int.MAX_VALUE }) {
            val choice = context.entryCopies[id]?.takeIf { it.copiedCard != null } ?: continue
            if (id in prepared.entryAuraHosts) continue
            val card = EffectCopyEntry.apply(state, id, choice).getEntity(id)?.get<CardComponent>() ?: continue
            if (!card.isAura) continue
            val hosts = legalHosts(state, id, card, controller, registry, finder, evaluator).filter { it !in entrants }
            val specified = (effect as? MoveCollectionEffect)?.attachTo
            if (specified != null || hosts.isEmpty()) {
                val host = specified?.let { context.resolveTarget(it, state) }?.takeIf { it in hosts }
                prepared = prepared.copy(entryAuraHosts = prepared.entryAuraHosts + (id to host))
                continue
            }
            val paused = state.suspendForDecision(question(id, card.name, controller, hosts),
                EffectCopyAuraEntryContinuation(effect, prepared, id))
            return Preparation(prepared, EffectResult.from(paused))
        }
        return Preparation(prepared)
    }
}
