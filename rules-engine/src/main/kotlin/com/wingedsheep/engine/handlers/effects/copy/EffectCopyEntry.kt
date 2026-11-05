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
            // A granted copy of a bound object (Mystic Reflection) supersedes the entrant's own
            // choice; the transition installs it, asking for an order between grants by replay.
            if (GrantedEntryCopy.find(state, id, evaluator) != null) continue
            val card = state.getEntity(id)?.get<CardComponent>() ?: continue
            val replacement = registry.getCard(card.cardDefinitionId)?.script?.replacementEffects
                ?.filterIsInstance<EntersAsCopy>()?.firstOrNull { it.copyOf == null }
                ?.let { PermanentEntryReplacements.withConditionalExceptions(state, id, controller, it, evaluator) }
                ?: continue
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

    /**
     * The key under which the choice for the [index]-th token a token-copy effect mints rides
     * [EffectContext.entryCopies]. A minted token has no entity before it is created, so its
     * as-enters copy choice is keyed by creation order — the way
     * [EffectContext.entryAttackDefenders] keys the CR 508.4 defenders.
     */
    fun mintedKey(index: Int): EntityId = EntityId("minted-token-copy-$index")

    /**
     * The minted token's own printed "you may have it enter as a copy of …" — a token that's a copy
     * of a Clone *card* (Vizier of Many Faces embalmed from the graveyard) has the Clone's
     * as-enters replacement among its copiable values, so it gets the choice as it enters.
     */
    fun mintedReplacement(card: CardComponent, registry: CardRegistry?): EntersAsCopy? =
        registry?.getCard(card.cardDefinitionId)?.script?.replacementEffects
            ?.filterIsInstance<EntersAsCopy>()?.firstOrNull { it.copyOf == null }

    /**
     * Ask, before any token exists, what the next unanswered one of [count] minted copies enters as
     * a copy of; the answer accumulates under [mintedKey] and [effect] is executed again from the top
     * ([EffectCopyEntryContinuation]). Null once every token has an answer, or has no candidate.
     * A [EntersAsCopy.condition] here is read with no entrant object yet, so only a condition that
     * doesn't look at the source (Protean Raider's raid) can hold. A granted entry copy waiting for
     * the token (Mystic Reflection) still applies after this choice and wins — one of the two orders
     * CR 616.1 allows, though the controller isn't asked which.
     */
    fun prepareMinted(
        state: GameState,
        effect: Effect,
        context: EffectContext,
        controllerId: EntityId,
        tokenName: String,
        replacement: EntersAsCopy,
        count: Int,
        evaluator: PredicateEvaluator,
    ): EffectResult? {
        for (index in 0 until count) {
            val key = mintedKey(index)
            if (key in context.entryCopies) continue
            // No mana was spent to cast a token.
            val candidates = PermanentEntryReplacements.entersAsCopyCandidates(state, key, controllerId, replacement, evaluator)
                .filter { !replacement.filterByTotalManaSpent || (state.getEntity(it)?.get<CardComponent>()?.manaValue ?: 0) == 0 }
            if (candidates.isEmpty()) continue
            val question = { decisionId: String -> SelectCardsDecision(
                id = decisionId, playerId = controllerId,
                prompt = "Choose a ${replacement.copyFilter.description} for $tokenName to copy",
                context = DecisionContext(sourceId = context.sourceId, sourceName = tokenName, phase = DecisionPhase.RESOLUTION),
                options = candidates, minSelections = if (replacement.optional) 0 else 1,
                maxSelections = 1, useTargetingUI = replacement.copyFromZone == Zone.BATTLEFIELD,
            ) }
            return EffectResult.from(state.suspendForDecision(question,
                EffectCopyEntryContinuation(effect, context, key, replacement)))
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
