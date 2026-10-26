package com.wingedsheep.engine.handlers.effects.copy

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

/** A null copied identity records a declined choice without changing the card in its old zone. */
@Serializable
data class EntryCopyChoice(
    val replacement: EntersAsCopy,
    val copiedCard: CardComponent? = null,
    val copiedEntity: EntityId? = null,
) {
    /** Whether [other] copies the same object under the same copy effect (identity values aside). */
    fun sameCopyAs(other: EntryCopyChoice): Boolean =
        copiedEntity == other.copiedEntity && replacement == other.replacement
}

/** Prepare all choices against the pre-entry battlefield, then replay just the move instruction. */
object EffectCopyEntry {
    fun prepare(
        state: GameState,
        effect: Effect,
        context: EffectContext,
        entrants: Map<EntityId, EntityId>,
        registry: CardRegistry,
        evaluator: PredicateEvaluator,
    ): EffectResult? {
        val playerOrder = state.apnapOrder.withIndex().associate { it.value to it.index }
        for ((id, controller) in entrants.entries.sortedBy { playerOrder[it.value] ?: Int.MAX_VALUE }) {
            if (id in context.entryCopies || id in state.getBattlefield()) continue
            val card = state.getEntity(id)?.get<CardComponent>() ?: continue
            // A granted copy of a bound object (Mystic Reflection) supersedes the entrant's own
            // choice; the transition installs it. Grants copying different objects are ordered by
            // the entrant's controller (CR 616.1): the one it applies last is the entry copy.
            val granted = GrantedEntryCopy.find(state, id, evaluator)
            if (granted != null) {
                if (!granted.needsOrder) continue
                return EffectResult.from(state.suspendForDecision(
                    GrantedEntryCopy.question(state, granted, id, card.name, controller),
                    GrantedEntryCopyOrderContinuation(id, granted.options, EffectReplay(effect, context))))
            }
            val replacement = registry.getCard(card.cardDefinitionId)?.script?.replacementEffects
                ?.filterIsInstance<EntersAsCopy>()?.firstOrNull { it.copyOf == null } ?: continue
            val candidates = PermanentEntryReplacements.entersAsCopyCandidates(
                state, id, controller, replacement, evaluator
            ).filter { candidate ->
                // No mana was spent to cast a permanent arriving by a zone-moving effect.
                !replacement.filterByTotalManaSpent ||
                    (state.getEntity(candidate)?.get<CardComponent>()?.manaValue ?: 0) == 0
            }
            if (candidates.isEmpty()) continue
            val question = { decisionId: String -> SelectCardsDecision(
                id = decisionId, playerId = controller,
                prompt = "Choose a ${replacement.copyFilter.description} for ${card.name} to copy",
                context = DecisionContext(sourceId = id, sourceName = card.name, phase = DecisionPhase.RESOLUTION),
                options = candidates, minSelections = if (replacement.optional) 0 else 1,
                maxSelections = 1, useTargetingUI = replacement.copyFromZone == Zone.BATTLEFIELD,
            ) }
            return EffectResult.from(state.suspendForDecision(question,
                EffectCopyEntryContinuation(effect, context, id, replacement)))
        }
        return null
    }

    /** Install the selected identity only when the zone transition actually reaches the battlefield. */
    fun apply(state: GameState, id: EntityId, choice: EntryCopyChoice): GameState {
        val target = choice.copiedCard ?: return state
        val original = state.getEntity(id)?.get<CardComponent>() ?: return state
        val copied = copiedIdentity(original, choice) ?: return state
        return state.updateEntity(id) { tagCopyDuration(it.with(copied).with(CopyOfComponent(
            originalCardDefinitionId = original.cardDefinitionId,
            copiedCardDefinitionId = target.cardDefinitionId, originalCardComponent = original)), choice.replacement.duration) }
    }

    /** [original] as a copy of [choice]'s copied object, its "except …" riders applied (CR 707.9b). */
    fun copiedIdentity(original: CardComponent, choice: EntryCopyChoice): CardComponent? {
        val target = choice.copiedCard ?: return null
        val r = choice.replacement
        val exceptions = r.exceptions.over(com.wingedsheep.sdk.scripting.effects.CopyExceptions(
            nameOverride = r.nameOverride, addedKeywords = r.additionalKeywords.toSet(),
            addedSubtypes = r.additionalSubtypes.map { com.wingedsheep.sdk.core.Subtype(it) }.toSet(),
            addedColors = r.additionalColors, powerOverride = r.powerOverride, toughnessOverride = r.toughnessOverride,
        ))
        return CopyExceptionApplier.apply(target.copy(ownerId = original.ownerId,
            isDoubleFaced = original.isDoubleFaced), exceptions,
            copierColors = original.colors, copierStats = original.baseStats)
    }

    /**
     * Mark an as-enters copy that lasts only "until end of turn" (Cursed Mirror) with the cleanup
     * revert marker the group copy effects already use; a permanent copy is left untagged.
     */
    fun tagCopyDuration(container: ComponentContainer, duration: Duration): ComponentContainer =
        if (duration == Duration.EndOfTurn) container.with(RevertCopyAtEndOfTurnComponent) else container
}
