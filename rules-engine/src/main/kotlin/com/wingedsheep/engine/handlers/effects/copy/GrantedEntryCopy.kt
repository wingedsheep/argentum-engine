package com.wingedsheep.engine.handlers.effects.copy

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.copiableCardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Granted "enters as a copy of *that* object" replacements — an [EntersAsCopy] with a bound
 * [EntersAsCopy.copyOf] recorded in [GameState.grantedReplacementEffects] ("the next time one or more
 * creatures or planeswalkers enter this turn, they enter as copies of the chosen creature" — Mystic
 * Reflection). Unlike a permanent's own as-enters copy there is nothing to choose, so every entry path
 * reads it the same way: the permanent-spell resolver, the effect move pipeline (`EffectCopyEntry`,
 * `EffectEntryChoices`, the zone transition) and the token / direct-insertion paths.
 *
 * Copy effects are applied before the entrant's other entry replacements (CR 616.1c), and those
 * others are then read from the permanent as it would exist with the copy applied (CR 614.12). So
 * the copied identity is installed first, and the entrant's own "enters as a copy" choice is not
 * asked: whichever order the two copy effects applied in, it would end up a copy of the bound object.
 *
 * A [Duration.NextUse] grant is spent once it modifies an entry; it keeps applying to the objects
 * entering in that same event and is purged at the event boundary
 * ([com.wingedsheep.engine.replacement.ActiveReplacements.purgeSpent]). Every grant that applies to
 * an entry is applied to it (CR 616.1f) and so spent by it. When grants bound to different objects
 * apply to one entrant, the copy shown is the most recently granted one's: the entrant's controller
 * would order them (CR 616.1c), and that ordering choice isn't offered.
 *
 * A face-down entrant is still modified: it becomes a copy under its face-down status (CR 708.10) —
 * it keeps the face-down characteristics, and the copied values show if it is turned face up.
 */
object GrantedEntryCopy {

    /**
     * The granted copies that modify an entrant: every applying grant's index, and the identity
     * shown — the most recently granted one's.
     */
    data class Found(val grantIndices: List<Int>, val choice: EntryCopyChoice)

    /** The granted copy replacements that modify [entityId]'s entry, or null when none applies. */
    fun find(state: GameState, entityId: EntityId, evaluator: PredicateEvaluator): Found? {
        val grants = state.grantedReplacementEffects
        if (grants.isEmpty()) return null
        var shown: EntryCopyChoice? = null
        val indices = mutableListOf<Int>()
        for (index in grants.indices.reversed()) {
            val grant = grants[index]
            val replacement = grant.replacement as? EntersAsCopy ?: continue
            val referenceId = (replacement.copyOf as? EffectTarget.SpecificEntity)?.entityId ?: continue
            val pattern = replacement.appliesTo as? EventPattern.ZoneChangeEvent ?: continue
            if (pattern.to != Zone.BATTLEFIELD) continue
            val context = PredicateContext(sourceId = grant.entityId, controllerId = grant.controllerId)
            if (!evaluator.matches(state, state.projectedState, entityId, pattern.filter, context)) continue
            val copied = referenceIdentity(state, referenceId, grant.referenceSnapshot) ?: continue
            indices += index
            if (shown == null) shown = EntryCopyChoice(replacement, copied, referenceId)
        }
        return shown?.let { Found(indices, it) }
    }

    /** Mark every one-shot grant that modified an entry as spent. */
    fun spend(state: GameState, found: Found): GameState {
        val grants = state.grantedReplacementEffects
        val toSpend = found.grantIndices.filter { i ->
            grants.getOrNull(i)?.let { it.duration is Duration.NextUse && !it.spent } == true
        }
        if (toSpend.isEmpty()) return state
        return state.copy(grantedReplacementEffects = grants.toMutableList().also { list ->
            for (i in toSpend) list[i] = list[i].copy(spent = true)
        })
    }

    /**
     * For an entrant that is inserted onto the battlefield directly (a token, a played land): if a
     * granted copy applies, install the copied identity with its static and replacement payload
     * ([withCopyIdentity]) before the entrant is placed, and spend the grant.
     */
    fun applyBeforeEntry(
        state: GameState,
        entityId: EntityId,
        registry: CardRegistry,
        evaluator: PredicateEvaluator,
    ): GameState {
        val found = find(state, entityId, evaluator) ?: return state
        val original = state.getEntity(entityId)?.get<CardComponent>() ?: return state
        val copied = EffectCopyEntry.copiedIdentity(original, found.choice) ?: return state
        val withCopy = state.updateEntity(entityId) { c ->
            EffectCopyEntry.tagCopyDuration(
                c.withCopyIdentity(copied, registry).with(CopyOfComponent(
                    originalCardDefinitionId = original.cardDefinitionId,
                    copiedCardDefinitionId = copied.cardDefinitionId,
                    originalCardComponent = original,
                )),
                found.choice.replacement.duration,
            )
        }
        return spend(withCopy, found)
    }

    /**
     * The copiable values of [referenceId] as the entrant enters (the ruling: "use the copiable
     * values of the creature at the time the permanents enter"): live while the same battlefield
     * visit lasts, otherwise as it last existed on the battlefield (CR 608.2h) — the departure
     * snapshot of that visit, else the one taken when the grant was made.
     */
    private fun referenceIdentity(
        state: GameState,
        referenceId: EntityId,
        captured: com.wingedsheep.engine.state.components.stack.EntitySnapshot?,
    ): CardComponent? {
        val visit = captured?.battlefieldEntryTimestamp
        val container = state.getEntity(referenceId)
        if (container != null && referenceId in state.getBattlefield() &&
            (visit == null || container.get<BattlefieldEntryTimestampComponent>()?.timestamp == visit)
        ) {
            return container.copiableCardComponent()
        }
        val departed = container?.get<LastKnownPermanentComponent>()?.snapshot
            ?.takeIf { it.copiableCard != null && it.battlefieldEntryTimestamp == visit }
        return (departed ?: captured)?.copiableCard
    }
}
