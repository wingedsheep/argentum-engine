package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TargetedByControllerThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.*

/**
 * Puts the non-cast stack objects on the stack — a copy of a spell (CR 707.10), a triggered
 * ability, an activated ability — and owns the "becomes the target" announcement every stack
 * object makes as it goes on the stack (CR 601.2c / 602.2b), including crime detection and the
 * Valiant "first time each turn" tracking.
 */
internal object StackPlacement {
    /**
     * Put a triggered ability on the stack.
     */
    fun putTriggeredAbility(
        state: GameState,
        ability: TriggeredAbilityOnStackComponent,
        targets: List<ChosenTarget> = emptyList(),
        targetRequirements: List<TargetRequirement> = emptyList(),
        /**
         * True when this ability fired because its own source creature was declared as an attacker
         * (a SELF-bound attacks trigger). Stamped onto the emitted [AbilityTriggeredEvent] so
         * Firebender Ascension's "attacking causes a triggered ability of that creature to trigger"
         * meta-trigger can key on it.
         */
        causedByAttack: Boolean = false
    ): ExecutionResult {
        // Create a new entity for the ability on the stack
        val (abilityId, stateWithId) = state.newEntity()

        var container = ComponentContainer.of(ability)
        if (targets.isNotEmpty()) {
            container = container.with(TargetsComponent.capture(state, targets, targetRequirements))
        }

        var newState = stateWithId.withEntity(abilityId, container)
        newState = newState.pushToStack(abilityId)
            .copy(priorityPassedBy = emptySet())

        // The only ability a face-down permanent can put on the stack is the ward its face-down
        // mode grants (CR 702.168a disguise / 701.58a cloak), and reporting `ability.sourceName`
        // for it announced exactly which card had just refused to be targeted.
        val sourceDisplayName = nameVisibleToAll(state, ability.sourceId, ability.sourceName)

        val events = mutableListOf<GameEvent>(
            AbilityTriggeredEvent(
                ability.sourceId,
                sourceDisplayName,
                ability.controllerId,
                ability.description,
                abilityEntityId = abilityId,
                causedByAttack = causedByAttack
            )
        )

        if (CrimeDetector.isCrime(newState, ability.controllerId, targets)) {
            events.add(CommitCrimeEvent(ability.controllerId, abilityId, sourceDisplayName))
            newState = recordCrime(newState, ability.controllerId)
        }

        if (targets.isNotEmpty()) {
            events.add(TargetsChosenEvent(ability.controllerId, abilityId, sourceDisplayName))
        }

        // Emit BecomesTargetEvent for each permanent, spell, or player target
        // Use abilityId (the entity on the stack) as source so ward can counter it
        for (target in targets.distinct()) {
            newState = emitBecomesTarget(
                newState, target, abilityId, ability.controllerId, events, sourceIsSpell = false
            )
        }

        return ExecutionResult.success(
            newState.tick(),
            events
        )
    }

    /**
     * Put a copy of a spell on the stack.
     *
     * Per rule 707.10, a copy of an instant or sorcery spell is itself a spell on the
     * stack with the original's characteristics. We clone the source's [CardComponent] and
     * [SpellOnStackComponent] onto a new entity, tag it with [CopyOfComponent], and push it.
     *
     * Per rule 707.10 a copy isn't cast — this emits a [SpellCopiedEvent], not a
     * [SpellCastEvent], so "whenever you cast a spell" triggers don't fire.
     *
     * Targets and modal choices default to inheriting from the source. Callers may override
     * them (e.g., Storm's per-copy retargeting).
     */
    fun putSpellCopy(
        state: GameState,
        sourceSpellId: EntityId,
        targets: List<ChosenTarget> = emptyList(),
        targetRequirements: List<TargetRequirement> = emptyList(),
        chosenModes: List<Int>? = null,
        modeTargetsOrdered: List<List<ChosenTarget>>? = null,
        modeTargetRequirements: Map<Int, List<TargetRequirement>>? = null,
        copyIndex: Int? = null,
        copyTotal: Int? = null,
        controllerId: EntityId? = null,
        exceptions: com.wingedsheep.sdk.scripting.effects.CopyExceptions = com.wingedsheep.sdk.scripting.effects.CopyExceptions.None,
        retainedTargetIndices: Set<Int>? = null
    ): ExecutionResult {
        val sourceContainer = state.getEntity(sourceSpellId)
            ?: return ExecutionResult.error(state, "Source spell not found: $sourceSpellId")
        // CR 707.10: a spell that can't be copied yields no copy. Succeed without change.
        if (sourceContainer.has<com.wingedsheep.engine.state.components.identity.CantBeCopiedComponent>()) {
            return ExecutionResult.success(state)
        }
        val sourceCard = sourceContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Source is not a card: $sourceSpellId")
        val sourceSpell = sourceContainer.get<SpellOnStackComponent>()
            ?: return ExecutionResult.error(state, "Source is not a spell on stack: $sourceSpellId")
        val sourceTargets = sourceContainer.get<TargetsComponent>()

        val (copyId, stateWithId) = state.newEntity()
        val copyController = controllerId ?: sourceSpell.casterId

        val effectiveModes = chosenModes ?: sourceSpell.chosenModes
        val effectiveModeTargets = modeTargetsOrdered ?: sourceSpell.modeTargetsOrdered
        val effectiveModeRequirements = modeTargetRequirements ?: sourceSpell.modeTargetRequirements

        // Determine final flat targets/requirements for the copy's TargetsComponent.
        val effectiveTargets = when {
            targets.isNotEmpty() -> targets
            effectiveModes.isNotEmpty() -> effectiveModeTargets.flatten()
            else -> sourceTargets?.targets ?: emptyList()
        }
        val effectiveRequirements = when {
            targetRequirements.isNotEmpty() -> targetRequirements
            effectiveModes.isNotEmpty() ->
                effectiveModes.flatMap { effectiveModeRequirements[it] ?: emptyList() }
            else -> sourceTargets?.targetRequirements ?: emptyList()
        }

        // Clone the card characteristics. The CardComponent keeps the same cardDefinitionId,
        // name, types, colors, mana cost, and spellEffect (707.10).
        val copiedCardComp = com.wingedsheep.engine.handlers.effects.copy.CopyExceptionApplier
            .apply(sourceCard, exceptions).copy(ownerId = copyController)

        // Clone cast-time state; per 707.10 the copy inherits every decision made for
        // the original. The data-class copy preserves: xValue, declaredCostSlot, wasBlightPaid,
        // wasWarped, wasEvoked, sacrificedPermanents (snapshots of P/T + subtypes), damageDistribution,
        // chosenCreatureType, exiledCardCount, beheldCards, convokedCreatures (CR 707.10: an
        // effect of the copy that refers to objects used to pay its costs uses the original's).
        // castFromZone is cleared: a copy isn't cast (707.10), so it wasn't cast from any zone —
        // "if this spell was cast from a graveyard" is false for it (Sevinne's Reclamation ruling).
        // Actual mana payment is not a copied decision: no mana was spent to cast the copy.
        // Clear every payment bucket and provenance map while retaining choices such as X.
        // The caster and modal fields may also change. Payment events (ManaSpentEvent, SpellCastEvent) are
        // deliberately not re-emitted — a copy isn't cast (707.10). For the same reason no mana
        // was spent on the copy, so a mana rider's entry keyword grant stays with the original.
        val copiedSpellComp = sourceSpell.copy(
            casterId = copyController,
            castFromZone = null,
            entryKeywordGrants = emptyList(),
            manaSpentWhite = 0,
            manaSpentBlue = 0,
            manaSpentBlack = 0,
            manaSpentRed = 0,
            manaSpentGreen = 0,
            manaSpentColorless = 0,
            manaSpentBySubtype = emptyMap(),
            manaSpentByCardType = emptyMap(),
            manaSpentSnow = 0,
            manaSpentOnXByColor = emptyMap(),
            chosenModes = effectiveModes,
            modeTargetsOrdered = effectiveModeTargets,
            modeTargetRequirements = effectiveModeRequirements,
            // A copy of a spliced spell copies the spliced text (Fork's 2004-12-01 ruling), whose
            // targets are the tail of the flat list. A copy keeps one slot per original target, so
            // each spliced card's slice keeps its size and is re-read from the copy's own targets.
            splicedTargetsOrdered = if (sourceSpell.splicedTargetsOrdered.isEmpty() || targets.isEmpty()) {
                sourceSpell.splicedTargetsOrdered
            } else {
                var cursor = effectiveTargets.size - sourceSpell.splicedTargetsOrdered.sumOf { it.size }
                sourceSpell.splicedTargetsOrdered.map { old ->
                    effectiveTargets.subList(cursor.coerceIn(0, effectiveTargets.size), (cursor + old.size).coerceIn(0, effectiveTargets.size))
                        .toList().also { cursor += old.size }
                }
            },
            damageDistribution = if (sourceSpell.damageDistribution.isNullOrEmpty() || (targets.isEmpty() && modeTargetsOrdered == null)) sourceSpell.damageDistribution else buildMap {
                sourceTargets?.targets.orEmpty().zip(effectiveTargets).forEach { (old, new) ->
                    fun id(t: ChosenTarget): EntityId = when (t) {
                        is ChosenTarget.Player -> t.playerId
                        is ChosenTarget.Permanent -> t.entityId
                        is ChosenTarget.Spell -> t.spellEntityId
                        is ChosenTarget.Card -> t.cardId
                    }
                    sourceSpell.damageDistribution?.get(id(old))?.let { amount -> put(id(new), (get(id(new)) ?: 0) + amount) }
                }
            }
        )

        var container = com.wingedsheep.engine.handlers.effects.copy.CopyExceptionApplier.withNumericKeywords(
            ComponentContainer.of(copiedCardComp, copiedSpellComp), sourceContainer, exceptions
        )
        if (effectiveTargets.isNotEmpty()) {
            val captured = TargetsComponent.capture(state, effectiveTargets, effectiveRequirements)
            // Explicit replacements capture the current object even when its entity id is unchanged.
            val inheritsAllTargets = targets.isEmpty() && modeTargetsOrdered == null
            val inherited = sourceTargets?.targets.orEmpty().mapIndexedNotNull { i, old ->
                val retained = retainedTargetIndices?.contains(i) ?: inheritsAllTargets
                if (old == effectiveTargets.getOrNull(i) && retained) old else null
            }.toSet()
            val ids = inherited.mapTo(mutableSetOf()) { target -> when (target) {
                is ChosenTarget.Player -> target.playerId
                is ChosenTarget.Permanent -> target.entityId
                is ChosenTarget.Spell -> target.spellEntityId
                is ChosenTarget.Card -> target.cardId
            } }
            container = container.with(captured.copy(
                targetEntryStamps = captured.targetEntryStamps.filterKeys { it !in ids } + sourceTargets?.targetEntryStamps.orEmpty().filterKeys { it in ids },
                targetObjectRefs = captured.targetObjectRefs.filterKeys { it !in ids } + sourceTargets?.targetObjectRefs.orEmpty().filterKeys { it in ids }
            ))
        }
        container = container.with(
            CopyOfComponent(
                originalCardDefinitionId = sourceCard.cardDefinitionId,
                copiedCardDefinitionId = sourceCard.cardDefinitionId
            )
        )

        var newState = stateWithId.withEntity(copyId, container)
        sourceContainer.get<com.wingedsheep.engine.mechanics.BestowedComponent>()?.let { bestowed ->
            // Bestow restores this identity on entry or when its target becomes illegal.
            // Restore the copy's exceptions as well as its printed characteristics.
            val original = com.wingedsheep.engine.handlers.effects.copy.CopyExceptionApplier
                .apply(bestowed.original, exceptions).copy(ownerId = copyController)
            newState = newState.updateEntity(copyId) { it.with(bestowed.copy(original = original)) }
        }
        newState = newState.pushToStack(copyId).copy(priorityPassedBy = emptySet())

        val events = mutableListOf<GameEvent>(
            SpellCopiedEvent(
                copyEntityId = copyId,
                cardName = copiedCardComp.name,
                controllerId = copyController,
                originalSpellId = sourceSpellId,
                copyIndex = copyIndex,
                copyTotal = copyTotal,
                manaValue = copiedCardComp.manaValue
            )
        )

        // Emit BecomesTargetEvent for each permanent, spell, or player target — the copy is its own
        // source on the stack (ward on the target can counter the copy independently).
        for (target in effectiveTargets.distinct()) {
            newState = emitBecomesTarget(newState, target, copyId, copyController, events, sourceIsSpell = true)
        }

        return ExecutionResult.success(newState.tick(), events)
    }

    /**
     * Put an activated ability on the stack.
     *
     * [emitActivationEvent] is true for a genuine activation. A **copy** of an activated ability is
     * *not* activated (CR 707.10), so the copy paths pass false to suppress the
     * [AbilityActivatedEvent] — otherwise placing the copy would itself re-fire
     * "whenever you activate an ability" triggers (e.g. Ertha Jo, Frontier Mentor would copy its own
     * copies endlessly). The copy still becomes a stack object with its own targets, so
     * `BecomesTargetEvent`/`TargetsChosenEvent` are still emitted below.
     */
    fun putActivatedAbility(
        state: GameState,
        ability: ActivatedAbilityOnStackComponent,
        targets: List<ChosenTarget> = emptyList(),
        targetRequirements: List<TargetRequirement> = emptyList(),
        emitActivationEvent: Boolean = true,
        costsTap: Boolean = false,
        isExhaust: Boolean = false,
        cantBeCopied: Boolean = false,
        isLoyalty: Boolean = false,
        loyaltyCountersRemoved: Int = 0,
    ): ExecutionResult {
        val (abilityId, stateWithId) = state.newEntity()

        var container = ComponentContainer.of(ability)
        if (targets.isNotEmpty()) {
            container = container.with(TargetsComponent.capture(state, targets, targetRequirements))
        }
        // CR 707.10e — "This ability can't be copied": tag the ability instance on the stack so a
        // copy-ability effect (e.g. Gogo, Master of Mimicry) makes no copy of it.
        if (cantBeCopied) {
            container = container.with(
                com.wingedsheep.engine.state.components.identity.CantBeCopiedComponent
            )
        }

        var newState = stateWithId.withEntity(abilityId, container)
        newState = newState.pushToStack(abilityId)
            .copy(priorityPassedBy = emptySet())

        val events = mutableListOf<GameEvent>()
        if (emitActivationEvent) {
            // Abilities reaching the stack are never mana abilities (CR 605.3 — mana abilities
            // resolve without the stack). costsTap lets the {T}-in-cost trigger family distinguish
            // tap-cost abilities (which it must skip) from non-tap ones.
            events.add(
                AbilityActivatedEvent(
                    ability.sourceId,
                    ability.sourceName,
                    ability.controllerId,
                    abilityEntityId = abilityId,
                    costsTap = costsTap,
                    isManaAbility = false,
                    isExhaust = isExhaust,
                    isLoyalty = isLoyalty,
                    loyaltyCountersRemoved = loyaltyCountersRemoved,
                )
            )
        }

        if (CrimeDetector.isCrime(newState, ability.controllerId, targets)) {
            events.add(CommitCrimeEvent(ability.controllerId, abilityId, ability.sourceName))
            newState = recordCrime(newState, ability.controllerId)
        }

        if (targets.isNotEmpty()) {
            events.add(TargetsChosenEvent(ability.controllerId, abilityId, ability.sourceName))
        }

        // Emit BecomesTargetEvent for each permanent, spell, or player target
        // Use abilityId (the entity on the stack) as source so ward can counter it
        for (target in targets.distinct()) {
            newState = emitBecomesTarget(
                newState, target, abilityId, ability.controllerId, events, sourceIsSpell = false
            )
        }

        return ExecutionResult.success(
            newState.tick(),
            events
        )
    }

    /**
     * Record that [playerId] committed a crime this turn (CR Outlaws of Thunder Junction). Folded
     * in at every [CommitCrimeEvent] emit site so the `PlayerCommittedCrimeThisTurn` condition (e.g.
     * Seize the Secrets' cost reduction) can read it. Cleared at each turn boundary by `TurnManager`.
     */
    fun recordCrime(state: GameState, playerId: EntityId): GameState =
        if (playerId in state.playersWhoCommittedCrimeThisTurn) state
        else state.copy(playersWhoCommittedCrimeThisTurn = state.playersWhoCommittedCrimeThisTurn + playerId)

    /**
     * Emit a [BecomesTargetEvent] for a permanent, spell, or player target (CR 601.2c — "The chosen
     * objects and/or players each become a target of that spell"). A [ChosenTarget.Card] (a card
     * targeted in a non-battlefield zone) still emits nothing: no printed "becomes the target"
     * trigger reaches into those zones, and the trigger side has no vocabulary to ask for it.
     * Returns the updated state.
     *
     * Spell targets are left out of the "targeted by this controller this turn" tracking (Valiant's
     * "first time each turn", and Angelic Cub's controller-blind reading of it) and always carry
     * both first-time flags `true`: a spell's stack entity can be
     * reused as the resolved permanent's entity, so marking it would leak a stale flag onto the
     * permanent. Permanents and players are tracked; `CleanupPhaseManager` clears the component for
     * every entity, players included.
     *
     * Callers emit once per **distinct** target: an object chosen for several instances of the word
     * "target" still becomes the target of that spell or ability once, so its becomes-target
     * triggers fire once (the heroic ruling: "only once per spell, even if that spell targets the
     * creature … multiple times").
     *
     * [sourceIsSpell] is required rather than defaulted so every call site has to state whether a
     * spell or an ability did the targeting — `spellsOnly` / `abilitiesOnly` read nothing else.
     */
    fun emitBecomesTarget(
        state: GameState,
        target: ChosenTarget,
        sourceEntityId: EntityId,
        controllerId: EntityId,
        events: MutableList<GameEvent>,
        sourceIsSpell: Boolean
    ): GameState {
        val isSpell = target is ChosenTarget.Spell
        val isPlayer = target is ChosenTarget.Player
        val targetEntityId = when (target) {
            is ChosenTarget.Permanent -> target.entityId
            is ChosenTarget.Spell -> target.spellEntityId
            is ChosenTarget.Player -> target.playerId
            is ChosenTarget.Card -> return state
        }
        val targetName = if (isPlayer) {
            state.getEntity(targetEntityId)?.get<PlayerComponent>()?.name ?: "Unknown"
        } else {
            state.getEntity(targetEntityId)?.get<CardComponent>()?.name ?: "Unknown"
        }
        val targetedBy = state.getEntity(targetEntityId)?.get<TargetedByControllerThisTurnComponent>()
        val firstTime = isSpell || targetedBy?.hasBeenTargetedBy(controllerId) != true
        val firstTimeByAnyone = isSpell || targetedBy?.controllerIds.isNullOrEmpty()
        events.add(
            BecomesTargetEvent(
                targetEntityId,
                targetName,
                sourceEntityId,
                controllerId,
                firstTime,
                firstTimeThisTurn = firstTimeByAnyone,
                targetIsSpell = isSpell,
                sourceIsSpell = sourceIsSpell,
                targetIsPlayer = isPlayer
            )
        )
        return if (isSpell) state else markTargetedByController(state, targetEntityId, controllerId)
    }

    // =========================================================================
    // Valiant / "first time targeted" tracking
    // =========================================================================

    /**
     * Mark the target entity as having been targeted by the given controller this turn.
     */
    private fun markTargetedByController(state: GameState, targetId: EntityId, controllerId: EntityId): GameState {
        return state.updateEntity(targetId) { container ->
            val existing = container.get<TargetedByControllerThisTurnComponent>()
                ?: TargetedByControllerThisTurnComponent()
            container.with(existing.withController(controllerId))
        }
    }
}
