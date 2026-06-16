package com.wingedsheep.engine.mechanics.stack
import com.wingedsheep.sdk.dsl.Patterns

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.EffectHandler
import com.wingedsheep.engine.mechanics.FlashbackGrants
import com.wingedsheep.engine.mechanics.HarmonizeGrants
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TargetedByControllerThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.battlefield.CastFromHandComponent
import com.wingedsheep.engine.state.components.battlefield.WarpedComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CantBeCounteredComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.HasMorphAbilityComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.identity.ExileAfterResolveComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.identity.PlottedComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.engine.state.permissions.removeMayPlayPermissionsForCard
import com.wingedsheep.sdk.scripting.conditions.SourcePlottedOnPriorTurn
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GrantCantBeCountered
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.WarpExileEffect
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.engine.handlers.effects.EntersWithCountersHelper
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.mechanics.targeting.HexproofSuppression
import com.wingedsheep.engine.mechanics.targeting.PlayerTargetRestriction
import com.wingedsheep.engine.state.components.battlefield.CantBeTargetedByOpponentAbilitiesComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.*

/**
 * Manages the stack: casting spells, activating abilities, and resolution.
 *
 * Handles:
 * - Putting spells on the stack
 * - Putting triggered abilities on the stack
 * - Putting activated abilities on the stack
 * - Resolving the top item
 * - Target validation on resolution
 * - Countering spells
 */
class StackResolver(
    private val cardRegistry: CardRegistry,
    private val effectHandler: EffectHandler = EffectHandler(cardRegistry = cardRegistry),
    private val staticAbilityHandler: StaticAbilityHandler = StaticAbilityHandler(cardRegistry),
    private val predicateEvaluator: PredicateEvaluator = PredicateEvaluator()
) {

    // =========================================================================
    // Casting Spells
    // =========================================================================

    /**
     * Put a spell on the stack.
     *
     * @param castFaceDown If true, cast as a face-down 2/2 creature (morph). The spell
     *                     will resolve as a face-down creature with FaceDownComponent
     *                     and MorphDataComponent.
     * @param damageDistribution Pre-chosen damage distribution for DividedDamageEffect spells
     */
    fun castSpell(
        state: GameState,
        cardId: EntityId,
        casterId: EntityId,
        targets: List<ChosenTarget> = emptyList(),
        xValue: Int? = null,
        sacrificedPermanents: List<PermanentSnapshot> = emptyList(),
        castFaceDown: Boolean = false,
        damageDistribution: Map<EntityId, Int>? = null,
        targetRequirements: List<TargetRequirement> = emptyList(),
        chosenCreatureType: String? = null,
        exiledCardCount: Int = 0,
        additionalCostBlightAmount: Int = 0,
        wasKicked: Boolean = false,
        wasBlightPaid: Boolean = false,
        wasWarped: Boolean = false,
        wasEvoked: Boolean = false,
        wasImpending: Boolean = false,
        wasSneaked: Boolean = false,
        sneakAttackDefenderId: EntityId? = null,
        chosenModes: List<Int> = emptyList(),
        modeTargetsOrdered: List<List<ChosenTarget>> = emptyList(),
        modeTargetRequirements: Map<Int, List<TargetRequirement>> = emptyMap(),
        modeDamageDistribution: Map<Int, Map<EntityId, Int>> = emptyMap(),
        totalManaSpent: Int = 0,
        beheldCards: List<EntityId> = emptyList(),
        chosenEntitySnapshots: List<PermanentSnapshot> = emptyList(),
        manaSpentWhite: Int = 0,
        manaSpentBlue: Int = 0,
        manaSpentBlack: Int = 0,
        manaSpentRed: Int = 0,
        manaSpentGreen: Int = 0,
        manaSpentColorless: Int = 0,
        manaSpentOnXByColor: Map<Color, Int> = emptyMap(),
        faceIndex: Int? = null,
        paidWithTreasureMana: Boolean = false,
        castTimeFlags: Set<String> = emptySet()
    ): ExecutionResult {
        val container = state.getEntity(cardId)
            ?: return ExecutionResult.error(state, "Card not found: $cardId")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card: $cardId")

        // Determine which zone the spell is being cast from (before removal)
        val castFromZone = findCastFromZone(state, cardId, casterId)

        // Remove from current zone (typically hand)
        var newState = removeFromCurrentZone(state, cardId, casterId)
        if (castFaceDown) {
            newState = clearRevealedMorphsInHand(newState, casterId)
        }

        // Build the flat target union for choose-N modal spells (Rule 700.2 / 601.2c).
        // TargetsComponent holds the union so existing target-arrow rendering and resolution-time
        // re-validation keep working; per-mode breakdown lives on SpellOnStackComponent.
        val effectiveTargets = if (modeTargetsOrdered.isNotEmpty()) {
            modeTargetsOrdered.flatten()
        } else {
            targets
        }
        val effectiveTargetRequirements = if (modeTargetRequirements.isNotEmpty() && targetRequirements.isEmpty()) {
            chosenModes.flatMap { modeTargetRequirements[it] ?: emptyList() }
        } else {
            targetRequirements
        }

        // Add spell components
        newState = newState.updateEntity(cardId) { c ->
            var updated = c.with(SpellOnStackComponent(
                casterId = casterId,
                xValue = xValue,
                wasKicked = wasKicked,
                wasBlightPaid = wasBlightPaid,
                chosenModes = chosenModes,
                modeTargetsOrdered = modeTargetsOrdered,
                modeTargetRequirements = modeTargetRequirements,
                modeDamageDistribution = modeDamageDistribution,
                sacrificedPermanents = sacrificedPermanents,
                castFaceDown = castFaceDown,
                damageDistribution = damageDistribution,
                chosenCreatureType = chosenCreatureType,
                exiledCardCount = exiledCardCount,
                additionalCostBlightAmount = additionalCostBlightAmount,
                castFromZone = castFromZone,
                wasWarped = wasWarped,
                wasEvoked = wasEvoked,
                wasImpending = wasImpending,
                wasSneaked = wasSneaked,
                sneakAttackDefenderId = sneakAttackDefenderId,
                beheldCards = beheldCards,
                chosenEntitySnapshots = chosenEntitySnapshots,
                manaSpentWhite = manaSpentWhite,
                manaSpentBlue = manaSpentBlue,
                manaSpentBlack = manaSpentBlack,
                manaSpentRed = manaSpentRed,
                manaSpentGreen = manaSpentGreen,
                manaSpentColorless = manaSpentColorless,
                manaSpentOnXByColor = manaSpentOnXByColor,
                faceIndex = faceIndex,
                castTimeFlags = castTimeFlags
            ))
            if (effectiveTargets.isNotEmpty()) {
                updated = updated.with(TargetsComponent(effectiveTargets, effectiveTargetRequirements))
            }
            // Add morph data for creatures with morph (needed for face-down casting and
            // for effects like Backslide that target "creature with a morph ability")
            val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
            val morphAbility = cardDef?.keywordAbilities?.filterIsInstance<KeywordAbility.Morph>()?.firstOrNull()
            if (morphAbility != null) {
                updated = updated.with(MorphDataComponent(
                    morphCost = morphAbility.morphCost,
                    originalCardDefinitionId = cardComponent.cardDefinitionId,
                    faceUpEffect = morphAbility.faceUpEffect
                ))
            }
            if (castFaceDown) {
                updated = updated.without<RevealedToComponent>()
            }
            updated
        }

        // Commander tax bookkeeping (CR 903.8): increment castsFromCommandZone on cast-commit so
        // that countered commanders still pay an escalating tax next time. Done after payment is
        // complete (the handler has already settled the mana cost) but before the spell is pushed
        // onto the stack — i.e. the cast is "committed" the moment the spell becomes a real
        // game object on the stack.
        if (castFromZone == Zone.COMMAND) {
            newState = newState.updateEntity(cardId) { c ->
                val commander = c.get<com.wingedsheep.engine.state.components.identity.CommanderComponent>()
                if (commander != null) {
                    c.with(commander.copy(castsFromCommandZone = commander.castsFromCommandZone + 1))
                } else {
                    c
                }
            }
        }

        // Push to stack and reset priority passes (new stack item requires fresh round of passes)
        newState = newState.pushToStack(cardId)
            .copy(priorityPassedBy = emptySet())

        // Consume one-shot free-cast permissions used to play this spell. If the
        // spell is later countered or fizzles and ExileAfterResolveComponent sends
        // it back to exile, the permission must already be gone — otherwise the
        // controller could re-cast the same card repeatedly (e.g. Daring Waverider's
        // free cast resurfacing every time the granted spell is countered).
        // "Permanent" permissions (e.g. Kheru Spellsnatcher's "for as long as it
        // remains exiled" grant) are left intact.
        newState = newState.updateEntity(cardId) { c ->
            var updated = c
            val payCost = c.get<PlayWithoutPayingCostComponent>()
            if (payCost != null && !payCost.permanent) {
                updated = updated.without<PlayWithoutPayingCostComponent>()
            }
            updated = updated.without<com.wingedsheep.engine.state.components.identity.PlayWithCostIncreaseComponent>()
            updated
        }
        // Drop this card from one-shot may-play grants. Permanent grants survive
        // (e.g. Adventure / Warp / Possibility Technician) and are stripped on resolve.
        // Multi-card permissions (Etali / Narset / Mind's Desire) keep authorising the
        // remaining cards — only the cast card loses its grant.
        newState = newState.copy(
            mayPlayPermissions = newState.mayPlayPermissions.mapNotNull { permission ->
                if (permission.permanent || cardId !in permission.cardIds) {
                    permission
                } else {
                    val remaining = permission.cardIds - cardId
                    if (remaining.isEmpty()) null else permission.copy(cardIds = remaining)
                }
            }
        )

        // Prepared (Secrets of Strixhaven): casting the prepare-spell copy unprepares its source
        // creature. Strip the source's PreparedComponent and consume the (permanent) cast-from-exile
        // permission for this copy so it can't be cast again — the copy itself is on the stack and
        // ceases to exist on resolution (CopyOfComponent), or the source's leave-battlefield cleanup
        // removes it if it never resolves.
        val prepareCopyComp = state.getEntity(cardId)
            ?.get<com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent>()
        if (prepareCopyComp != null) {
            newState = newState.updateEntity(prepareCopyComp.sourceId) { c ->
                c.without<com.wingedsheep.engine.state.components.battlefield.PreparedComponent>()
            }
            newState = newState.removeMayPlayPermissionsForCard(cardId)
        }

        // For face-down creatures, use a generic name in the event
        val eventName = if (castFaceDown) "Face-down creature" else cardComponent.name

        // Collect target names for the cast event log
        val targetNames = effectiveTargets.mapNotNull { target ->
            when (target) {
                is ChosenTarget.Permanent -> newState.getEntity(target.entityId)?.get<CardComponent>()?.name
                is ChosenTarget.Player -> if (target.playerId == casterId) "themselves" else "opponent"
                is ChosenTarget.Spell -> newState.getEntity(target.spellEntityId)?.get<CardComponent>()?.name
                    ?: "spell"
                is ChosenTarget.Card -> newState.getEntity(target.cardId)?.get<CardComponent>()?.name
            }
        }

        // Only count modes for triggers (Riku of Many Paths' "Whenever you cast a
        // modal spell" → IsModal predicate + MODES_CHOSEN_ON_TRIGGERING_SPELL) when
        // the spell's effect is a *true* modal — printed "Choose one — • X • Y"
        // wording. Mechanics like Gift use [ModalEffect] as an implementation
        // shortcut for a yes/no cost choice but are not modal in MTG terms; those
        // construct via `Patterns.Mechanic.giftSpell` (or set `countsAsModalSpell =
        // false` directly), which zeroes the count here.
        val countsAsModalForTriggers = run {
            val script = cardRegistry.getCard(cardComponent.cardDefinitionId)?.script
            val modal = script?.spellEffect as? com.wingedsheep.sdk.scripting.effects.ModalEffect
            modal?.countsAsModalSpell ?: false
        }
        val reportedChosenModesCount = if (countsAsModalForTriggers) chosenModes.size else 0

        val events = mutableListOf<GameEvent>(
            SpellCastEvent(
                spellEntityId = cardId,
                cardName = eventName,
                casterId = casterId,
                targetNames = targetNames,
                xValue = xValue,
                wasKicked = wasKicked,
                totalManaSpent = totalManaSpent,
                distinctColorsSpent =
                    com.wingedsheep.engine.handlers.ManaSpentReader.distinctColorsSpent(newState, cardId),
                paidWithTreasureMana = paidWithTreasureMana,
                chosenModesCount = reportedChosenModesCount,
                manaValue = cardComponent.manaValue
            )
        )

        // Crime detection (CR Outlaws of Thunder Junction). Emit at most once per cast,
        // regardless of how many opponent-controlled targets the spell chose.
        if (CrimeDetector.isCrime(newState, casterId, effectiveTargets)) {
            events.add(CommitCrimeEvent(casterId, cardId, eventName))
            newState = recordCrime(newState, casterId)
        }

        // "Whenever a player chooses one or more targets" (Psychic Battle). Emit once per cast
        // when the spell chose at least one target.
        if (effectiveTargets.isNotEmpty()) {
            events.add(TargetsChosenEvent(casterId, cardId, eventName))
        }

        // Emit BecomesTargetEvent for each permanent or spell target (Rule 601.2c)
        // Also track targeting for Valiant ("first time each turn")
        for (target in effectiveTargets) {
            newState = emitBecomesTarget(newState, target, cardId, casterId, events)
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
    private fun recordCrime(state: GameState, playerId: EntityId): GameState =
        if (playerId in state.playersWhoCommittedCrimeThisTurn) state
        else state.copy(playersWhoCommittedCrimeThisTurn = state.playersWhoCommittedCrimeThisTurn + playerId)

    /**
     * Emit a [BecomesTargetEvent] for a permanent or spell target. Players and other target kinds
     * emit nothing. Returns the updated state.
     *
     * Only permanents participate in the "targeted by this controller this turn" tracking (Valiant's
     * "first time each turn"): a spell's stack entity can be reused as the resolved permanent's
     * entity, so marking it would leak a stale flag onto the permanent. Spell-target events carry
     * `firstTime = true` and leave the tracking untouched.
     */
    private fun emitBecomesTarget(
        state: GameState,
        target: ChosenTarget,
        sourceEntityId: EntityId,
        controllerId: EntityId,
        events: MutableList<GameEvent>
    ): GameState {
        val isSpell = target is ChosenTarget.Spell
        val targetEntityId = when (target) {
            is ChosenTarget.Permanent -> target.entityId
            is ChosenTarget.Spell -> target.spellEntityId
            else -> return state
        }
        val targetName = state.getEntity(targetEntityId)?.get<CardComponent>()?.name ?: "Unknown"
        val firstTime = isSpell || !hasBeenTargetedByController(state, targetEntityId, controllerId)
        events.add(
            BecomesTargetEvent(
                targetEntityId,
                targetName,
                sourceEntityId,
                controllerId,
                firstTime,
                targetIsSpell = isSpell
            )
        )
        return if (isSpell) state else markTargetedByController(state, targetEntityId, controllerId)
    }

    /**
     * Put a triggered ability on the stack.
     */
    fun putTriggeredAbility(
        state: GameState,
        ability: TriggeredAbilityOnStackComponent,
        targets: List<ChosenTarget> = emptyList(),
        targetRequirements: List<TargetRequirement> = emptyList()
    ): ExecutionResult {
        // Create a new entity for the ability on the stack
        val (abilityId, stateWithId) = state.newEntity()

        var container = ComponentContainer.of(ability)
        if (targets.isNotEmpty()) {
            container = container.with(TargetsComponent(targets, targetRequirements))
        }

        var newState = stateWithId.withEntity(abilityId, container)
        newState = newState.pushToStack(abilityId)
            .copy(priorityPassedBy = emptySet())

        val events = mutableListOf<GameEvent>(
            AbilityTriggeredEvent(
                ability.sourceId,
                ability.sourceName,
                ability.controllerId,
                ability.description,
                abilityEntityId = abilityId
            )
        )

        if (CrimeDetector.isCrime(newState, ability.controllerId, targets)) {
            events.add(CommitCrimeEvent(ability.controllerId, abilityId, ability.sourceName))
            newState = recordCrime(newState, ability.controllerId)
        }

        if (targets.isNotEmpty()) {
            events.add(TargetsChosenEvent(ability.controllerId, abilityId, ability.sourceName))
        }

        // Emit BecomesTargetEvent for each permanent or spell target
        // Use abilityId (the entity on the stack) as source so ward can counter it
        for (target in targets) {
            newState = emitBecomesTarget(newState, target, abilityId, ability.controllerId, events)
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
        controllerId: EntityId? = null
    ): ExecutionResult {
        val sourceContainer = state.getEntity(sourceSpellId)
            ?: return ExecutionResult.error(state, "Source spell not found: $sourceSpellId")
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
        val copiedCardComp = sourceCard.copy(ownerId = copyController)

        // Clone cast-time state; per 707.10 the copy inherits every decision made for
        // the original. The data-class copy preserves: xValue, wasKicked, wasBlightPaid,
        // wasWarped, wasEvoked, sacrificedPermanents (snapshots of P/T + subtypes), damageDistribution,
        // chosenCreatureType, exiledCardCount, castFromZone, beheldCards, and the
        // manaSpent{White,Blue,Black,Red,Green,Colorless} colors. Only the caster
        // (copy controller) and modal fields (which the caller may retarget) are
        // overridden explicitly. Payment events (ManaSpentEvent, SpellCastEvent) are
        // deliberately not re-emitted — a copy isn't cast (707.10).
        val copiedSpellComp = sourceSpell.copy(
            casterId = copyController,
            chosenModes = effectiveModes,
            modeTargetsOrdered = effectiveModeTargets,
            modeTargetRequirements = effectiveModeRequirements
        )

        var container = ComponentContainer.of(copiedCardComp, copiedSpellComp)
        if (effectiveTargets.isNotEmpty()) {
            container = container.with(TargetsComponent(effectiveTargets, effectiveRequirements))
        }
        container = container.with(
            CopyOfComponent(
                originalCardDefinitionId = sourceCard.cardDefinitionId,
                copiedCardDefinitionId = sourceCard.cardDefinitionId
            )
        )

        var newState = stateWithId.withEntity(copyId, container)
        newState = newState.pushToStack(copyId).copy(priorityPassedBy = emptySet())

        val events = mutableListOf<GameEvent>(
            SpellCopiedEvent(
                copyEntityId = copyId,
                cardName = sourceCard.name,
                controllerId = copyController,
                originalSpellId = sourceSpellId,
                copyIndex = copyIndex,
                copyTotal = copyTotal
            )
        )

        // Emit BecomesTargetEvent for each permanent or spell target — the copy is its own
        // source on the stack (ward on the target can counter the copy independently).
        for (target in effectiveTargets) {
            newState = emitBecomesTarget(newState, target, copyId, copyController, events)
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
        emitActivationEvent: Boolean = true
    ): ExecutionResult {
        val (abilityId, stateWithId) = state.newEntity()

        var container = ComponentContainer.of(ability)
        if (targets.isNotEmpty()) {
            container = container.with(TargetsComponent(targets, targetRequirements))
        }

        var newState = stateWithId.withEntity(abilityId, container)
        newState = newState.pushToStack(abilityId)
            .copy(priorityPassedBy = emptySet())

        val events = mutableListOf<GameEvent>()
        if (emitActivationEvent) {
            events.add(
                AbilityActivatedEvent(
                    ability.sourceId,
                    ability.sourceName,
                    ability.controllerId,
                    abilityEntityId = abilityId
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

        // Emit BecomesTargetEvent for each permanent or spell target
        // Use abilityId (the entity on the stack) as source so ward can counter it
        for (target in targets) {
            newState = emitBecomesTarget(newState, target, abilityId, ability.controllerId, events)
        }

        return ExecutionResult.success(
            newState.tick(),
            events
        )
    }

    // =========================================================================
    // Resolution
    // =========================================================================

    /**
     * Resolve the top item on the stack.
     */
    fun resolveTop(state: GameState): ExecutionResult {
        val topId = state.getTopOfStack()
            ?: return ExecutionResult.error(state, "Stack is empty")

        val container = state.getEntity(topId)
            ?: return ExecutionResult.error(state, "Stack item not found: $topId")

        // Pop from stack
        val (_, poppedState) = state.popFromStack()

        // Determine what type of item this is
        return when {
            container.has<SpellOnStackComponent>() ->
                resolveSpell(poppedState, topId, container)

            container.has<TriggeredAbilityOnStackComponent>() ->
                resolveTriggeredAbility(poppedState, topId, container)

            container.has<ActivatedAbilityOnStackComponent>() ->
                resolveActivatedAbility(poppedState, topId, container)

            else ->
                ExecutionResult.error(state, "Unknown stack item type")
        }
    }

    /**
     * Resolve a spell.
     */
    private fun resolveSpell(
        state: GameState,
        spellId: EntityId,
        container: ComponentContainer
    ): ExecutionResult {
        val cardComponent = container.get<CardComponent>()
        val spellComponent = container.get<SpellOnStackComponent>()!!
        val targetsComponent = container.get<TargetsComponent>()

        // Validate targets if spell has any (including protection check - Rule 702.16)
        val sourceColors = cardComponent?.colors ?: emptySet()
        val sourceSubtypes = cardComponent?.typeLine?.subtypes?.map { it.value }?.toSet() ?: emptySet()
        // `resolvedTargets` is the compacted (drop-illegal) list used as `context.targets`
        // — same shape every executor has always seen. `alignedResolvedTargets` is a parallel
        // list the same length as the originally-chosen targets, with `null` in slots whose
        // target was dropped by 608.2b validation. It is forwarded to `buildNamedTargets`
        // so a sub-effect that references a now-illegal target through its declared
        // [EffectTarget.BoundVariable] (e.g. Diplomatic Relations' `myCreature` after its
        // FROM creature dies in response) resolves to `null` and fizzles, instead of
        // silently consuming the NEXT still-valid target whose position shifted forward
        // in the compacted list.
        val resolvedTargets: List<ChosenTarget>
        val alignedResolvedTargets: List<ChosenTarget?>
        if (targetsComponent != null && targetsComponent.targets.isNotEmpty()) {
            val validTargets = validateTargets(
                state, targetsComponent.targets, sourceColors, sourceSubtypes,
                spellComponent.casterId, targetsComponent.targetRequirements,
                sourceId = spellId,
                targetingSourceType = TargetingSourceType.SPELL,
                xValue = spellComponent.xValue
            )
            if (validTargets.isEmpty()) {
                // All targets invalid - spell fizzles
                return fizzleSpell(state, spellId, cardComponent, spellComponent)
            }
            resolvedTargets = validTargets
            alignedResolvedTargets = buildAlignedValidated(targetsComponent.targets, validTargets)
        } else {
            resolvedTargets = targetsComponent?.targets ?: emptyList()
            alignedResolvedTargets = resolvedTargets
        }

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Check if permanent or non-permanent.
        // Adventure / split face cast (CR 715 / 709) — when the spell was cast as a face, route
        // resolution by the face's type line. An Adventure (instant/sorcery) face on a creature
        // card must take the non-permanent path even though the card's primary characteristics
        // describe a creature.
        val faceTypeLine = spellComponent.faceIndex?.let { idx ->
            val def = cardComponent?.let { cardRegistry.getCard(it.name) }
            def?.cardFaces?.getOrNull(idx)?.typeLine
        }
        val resolvedTypeLine = faceTypeLine ?: cardComponent?.typeLine
        val isPermanent = resolvedTypeLine?.isPermanent ?: false

        if (isPermanent) {
            // Put permanent on battlefield
            val permanentResult = resolvePermanentSpell(newState, spellId, spellComponent, cardComponent)
            if (permanentResult.isPaused) {
                return ExecutionResult.paused(
                    permanentResult.state,
                    permanentResult.pendingDecision!!,
                    events + permanentResult.events
                )
            }
            newState = permanentResult.state
            events.addAll(permanentResult.events)
            val permanentName = cardComponent?.name ?: "Unknown"
            events.add(ResolvedEvent(spellId, permanentName))
            events.add(
                ZoneChangeEvent(
                    spellId,
                    permanentName,
                    null, // Was on stack
                    Zone.BATTLEFIELD,
                    cardComponent?.ownerId ?: spellComponent.casterId,
                    xValue = spellComponent.xValue
                )
            )
        } else {
            // Execute effects and put in graveyard
            val effectResult = resolveNonPermanentSpell(
                newState, spellId, spellComponent, cardComponent,
                resolvedTargets,
                alignedResolvedTargets
            )
            if (effectResult.isPaused) {
                // Effect paused for a decision (e.g., draw replacement prompt).
                // resolveNonPermanentSpell already moved spell to graveyard.
                val allEvents = events + effectResult.events +
                    ResolvedEvent(spellId, cardComponent?.name ?: "Unknown")
                return ExecutionResult.paused(
                    effectResult.state,
                    effectResult.pendingDecision!!,
                    allEvents
                )
            }
            newState = effectResult.newState
            events.addAll(effectResult.events)
            events.add(ResolvedEvent(spellId, cardComponent?.name ?: "Unknown"))
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Resolve a permanent spell - put it on the battlefield.
     * May pause for player input (e.g., Clone choosing a creature to copy).
     */
    private fun resolvePermanentSpell(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?
    ): ExecutionResult {
        val controllerId = spellComponent.casterId
        val ownerId = cardComponent?.ownerId ?: controllerId

        // Check for EntersAsCopy replacement effect before entering the battlefield
        val cardDef = cardComponent?.cardDefinitionId?.let { cardRegistry.getCard(it) }
        if (cardDef != null && !spellComponent.castFaceDown) {
            val entersAsCopy = cardDef.script.replacementEffects.filterIsInstance<EntersAsCopy>().firstOrNull()
            if (entersAsCopy != null) {
                // Find candidates to copy. Battlefield copies (Clone) read permanents in play;
                // graveyard copies (Superior Spider-Man) read creature cards across every graveyard.
                val copyFilter = entersAsCopy.copyFilter
                val copyFromGraveyard = entersAsCopy.copyFromZone == Zone.GRAVEYARD
                val candidatePool = if (copyFromGraveyard) {
                    state.turnOrder.flatMap { state.getGraveyard(it) }
                } else {
                    state.getBattlefield()
                }
                var candidates = candidatePool.filter { entityId ->
                    predicateEvaluator.matches(
                        state, state.projectedState, entityId, copyFilter,
                        PredicateContext(controllerId = controllerId)
                    )
                }

                // Filter by mana value ≤ total mana spent (for Mockingbird-style effects)
                if (entersAsCopy.filterByTotalManaSpent) {
                    val xValue = spellComponent.xValue ?: 0
                    // Total mana spent = X + non-X portion of mana cost
                    val baseNonXCost = cardComponent.manaCost.symbols
                        .filterNot { it is com.wingedsheep.sdk.core.ManaSymbol.X }
                        .sumOf { it.cmc }
                    val totalManaSpent = xValue + baseNonXCost
                    candidates = candidates.filter { entityId ->
                        val targetCard = state.getEntity(entityId)?.get<CardComponent>()
                        (targetCard?.manaValue ?: 0) <= totalManaSpent
                    }
                }

                if (candidates.isNotEmpty()) {
                    // Present the selection decision
                    val filterDesc = copyFilter.description
                    val whereDesc = if (copyFromGraveyard) "$filterDesc card in a graveyard" else "$filterDesc"
                    val decisionId = "clone-enters-${spellId.value}"
                    val decision = SelectCardsDecision(
                        id = decisionId,
                        playerId = controllerId,
                        prompt = if (entersAsCopy.optional) {
                            "You may choose a $whereDesc to copy"
                        } else {
                            "Choose a $whereDesc to copy"
                        },
                        context = DecisionContext(
                            sourceId = spellId,
                            sourceName = cardComponent.name,
                            phase = DecisionPhase.RESOLUTION
                        ),
                        options = candidates,
                        minSelections = if (entersAsCopy.optional) 0 else 1,
                        maxSelections = 1,
                        // Battlefield copies click permanents in-place; graveyard copies use the
                        // modal card-list overlay (graveyards aren't on the battlefield).
                        useTargetingUI = !copyFromGraveyard
                    )

                    // Push continuation
                    val continuation = CloneEntersContinuation(
                        decisionId = decisionId,
                        spellId = spellId,
                        controllerId = controllerId,
                        ownerId = ownerId,
                        castFaceDown = spellComponent.castFaceDown,
                        additionalSubtypes = entersAsCopy.additionalSubtypes,
                        additionalKeywords = entersAsCopy.additionalKeywords,
                        nameOverride = entersAsCopy.nameOverride,
                        powerOverride = entersAsCopy.powerOverride,
                        toughnessOverride = entersAsCopy.toughnessOverride,
                        exileCopiedCard = entersAsCopy.exileCopiedCard
                    )

                    val pausedState = state
                        .pushContinuation(continuation)
                        .withPendingDecision(decision)
                    return ExecutionResult.paused(pausedState, decision)
                }
                // No matching permanents on battlefield - fall through to enter as itself (0/0)
            }

            // Check for EntersWithChoice replacement effects (color first, then creature type, then creature)
            // Process in priority order: COLOR → CREATURE_TYPE → CREATURE_ON_BATTLEFIELD
            // When a card has multiple choices (e.g., Riptide Replicator: color + creature type),
            // the first one pauses; its continuation resumer chains to the next.
            val entersWithChoices = cardDef.script.replacementEffects.filterIsInstance<EntersWithChoice>()
            val firstChoice = entersWithChoices
                .sortedBy { it.choiceType.ordinal }
                .firstOrNull()
            if (firstChoice != null) {
                val result = pauseForEntersWithChoice(state, spellId, controllerId, ownerId, cardComponent, firstChoice)
                if (result != null) return result
                // null means choice couldn't be presented (e.g., no creatures on battlefield) — fall through
            }

            // Check for EntersWithRevealCounters replacement effect (Amplify mechanic)
            val revealCountersEffect = cardDef.script.replacementEffects.filterIsInstance<com.wingedsheep.sdk.scripting.EntersWithRevealCounters>().firstOrNull()
            if (revealCountersEffect != null) {
                // Find cards in the reveal source zone that match the effect's filter
                val revealZone = ZoneKey(controllerId, revealCountersEffect.revealSource)
                val predicateContext = PredicateContext(controllerId = controllerId, sourceId = spellId)
                val validCards = state.getZone(revealZone).filter { cardId ->
                    predicateEvaluator.matches(state, state.projectedState, cardId, revealCountersEffect.filter, predicateContext)
                }

                if (validCards.isNotEmpty()) {
                    val decisionId = "reveal-counters-enters-${spellId.value}"
                    val decision = SelectCardsDecision(
                        id = decisionId,
                        playerId = controllerId,
                        prompt = "Reveal cards from your ${revealCountersEffect.revealSource.name.lowercase()} that match ${cardComponent.name} (${revealCountersEffect.countersPerReveal} ${revealCountersEffect.counterType} counter${if (revealCountersEffect.countersPerReveal > 1) "s" else ""} each)",
                        context = DecisionContext(
                            sourceId = spellId,
                            sourceName = cardComponent.name,
                            phase = DecisionPhase.RESOLUTION
                        ),
                        options = validCards,
                        minSelections = 0,
                        maxSelections = validCards.size
                    )

                    val continuation = RevealCountersContinuation(
                        decisionId = decisionId,
                        spellId = spellId,
                        controllerId = controllerId,
                        ownerId = ownerId,
                        counterType = revealCountersEffect.counterType,
                        countersPerReveal = revealCountersEffect.countersPerReveal
                    )

                    val pausedState = state
                        .pushContinuation(continuation)
                        .withPendingDecision(decision)
                    return ExecutionResult.paused(pausedState, decision)
                }
                // No valid cards — enter normally without counters
            }

            // Check for EntersWithDevour replacement effect (CR 702.82, Devour variants).
            // Pauses for the controller to pick which permanents to sacrifice; the resumer
            // sacrifices them, places multiplier × count counters on the entering spell
            // entity, then completes the entry.
            val devourEffect = cardDef.script.replacementEffects
                .filterIsInstance<com.wingedsheep.sdk.scripting.EntersWithDevour>().firstOrNull()
            if (devourEffect != null) {
                val predicateContext = PredicateContext(controllerId = controllerId, sourceId = spellId)
                val candidates = state.getBattlefield().filter { entityId ->
                    if (entityId == spellId) return@filter false
                    // Use projected controller — control-changing effects (e.g. an opponent's
                    // Act of Treason on one of your lands) must be respected; you can only
                    // sacrifice permanents you currently control (CR 701.21a).
                    if (state.projectedState.getController(entityId) != controllerId) return@filter false
                    predicateEvaluator.matches(state, state.projectedState, entityId, devourEffect.sacrificeFilter, predicateContext)
                }

                if (candidates.isNotEmpty()) {
                    val devourLabel = devourEffect.description.substringBefore(" (")
                    val decisionId = "devour-enters-${spellId.value}"
                    val decision = SelectCardsDecision(
                        id = decisionId,
                        playerId = controllerId,
                        prompt = "$devourLabel: sacrifice any number of ${devourEffect.sacrificeFilter.description}s for ${cardComponent.name}",
                        context = DecisionContext(
                            sourceId = spellId,
                            sourceName = cardComponent.name,
                            phase = DecisionPhase.RESOLUTION
                        ),
                        options = candidates,
                        minSelections = 0,
                        maxSelections = candidates.size,
                        useTargetingUI = true
                    )

                    val continuation = DevourEntersContinuation(
                        decisionId = decisionId,
                        spellId = spellId,
                        controllerId = controllerId,
                        ownerId = ownerId,
                        multiplier = devourEffect.multiplier,
                        counterType = devourEffect.counterType.description
                    )

                    val pausedState = state
                        .pushContinuation(continuation)
                        .withPendingDecision(decision)
                    return ExecutionResult.paused(pausedState, decision)
                }
                // No valid permanents to sacrifice — enter with zero devour counters
            }
        }

        // Check for "pay life or enter tapped" (shock lands) before entering the battlefield
        if (cardDef != null && !spellComponent.castFaceDown) {
            val entersTapped = cardDef.script.replacementEffects.filterIsInstance<EntersTapped>().firstOrNull()
            if (entersTapped?.payLifeCost != null) {
                val decisionId = "pay-life-or-enter-tapped-spell-${spellId.value}"
                val decision = YesNoDecision(
                    id = decisionId,
                    playerId = controllerId,
                    prompt = "Pay ${entersTapped.payLifeCost} life to have ${cardComponent.name} enter untapped?",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    )
                )
                val continuation = PayLifeOrEnterTappedSpellContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    lifeCost = entersTapped.payLifeCost!!
                )
                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                return ExecutionResult.paused(pausedState, decision)
            }
        }

        // Normal permanent entry
        val (newState, enterEvents) = enterPermanentOnBattlefield(state, spellId, spellComponent, cardComponent, cardDef)
        val sagaEvents = if (cardDef != null && !spellComponent.castFaceDown && cardDef.isSaga) {
            listOf(CountersAddedEvent(spellId, "LORE", 1, cardDef.name))
        } else {
            emptyList()
        }
        return ExecutionResult.success(newState, enterEvents + sagaEvents)
    }

    /**
     * Complete the permanent entry to the battlefield (shared between normal resolution and clone continuation).
     */
    internal fun enterPermanentOnBattlefield(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?
    ): Pair<GameState, List<GameEvent>> {
        val controllerId = spellComponent.casterId

        // For Auras: get the target before removing TargetsComponent
        val auraTargetId = if (cardComponent?.isAura == true) {
            state.getEntity(spellId)?.get<TargetsComponent>()?.targets?.firstOrNull()?.let { target ->
                when (target) {
                    is ChosenTarget.Permanent -> target.entityId
                    else -> null
                }
            }
        } else null

        // Update entity: remove spell components, add permanent components.
        // CR 707.10f: a copy of a permanent spell becomes a token as it resolves.
        // Distinguish from "enters as a copy" effects (Clone, Mockingbird) which set
        // originalCardComponent for the revert-on-leave rule; those produce real
        // permanents, not tokens.
        val copyOf = state.getEntity(spellId)
            ?.get<com.wingedsheep.engine.state.components.identity.CopyOfComponent>()
        val resolvingAsSpellCopy = copyOf != null && copyOf.originalCardComponent == null
        var newState = state.updateEntity(spellId) { c ->
            var updated = c.without<SpellOnStackComponent>()
                .without<TargetsComponent>()
                .with(ControllerComponent(controllerId))

            if (resolvingAsSpellCopy) {
                updated = updated.with(TokenComponent)
            }

            // If cast face-down (morph), add FaceDownComponent and strip any
            // RevealedToComponent from hand-peek effects (zone change = new object)
            // MorphDataComponent was already added when the spell was cast
            if (spellComponent.castFaceDown) {
                updated = updated.with(FaceDownComponent)
                    .without<RevealedToComponent>()
            }

            // All permanents enter summoning sick (CR 302.6 / 508.1a — the control-continuity
            // check is about the permanent, not whether it was a creature the whole turn). Vehicles
            // and animated lands that become creatures mid-turn must inherit the marker too.
            // Downstream checks gate on isCreature/{T}-cost so this is harmless for lands and
            // non-creature artifacts until they become creatures (Crew, animate-land, etc.).
            updated = updated.with(SummoningSicknessComponent)

            // Track that this permanent entered the battlefield this turn
            updated = updated.with(EnteredThisTurnComponent)

            // Track if this permanent was cast from hand (for cards like Phage the Untouchable)
            if (spellComponent.castFromZone == Zone.HAND) {
                updated = updated.with(CastFromHandComponent)
            }

            // Track if this permanent was cast from a graveyard (for triggers that care about
            // creatures cast from graveyard — e.g., Twilight Diviner).
            if (spellComponent.castFromZone == Zone.GRAVEYARD) {
                updated = updated.with(com.wingedsheep.engine.state.components.battlefield.CastFromGraveyardComponent)
            }

            // Carry the cast-time choices durably onto the permanent (CR 601.2b choices ride the
            // stable entity onto the battlefield) so triggered/activated abilities can read "the X
            // / color / type / kicked-ness this was cast with" via DynamicAmount.CastX /
            // DynamicAmount.CastChoice / Conditions.CastChoice* for its whole life on the
            // battlefield, with no counter laundering. The bag is stripped when the permanent leaves
            // the battlefield (new object, CR 400.7) — see ZoneMovementUtils.stripBattlefieldComponents.
            //
            // Merge the *as-it-enters* choices already written by the EntersWithChoice resumers
            // (color/type/mode/…) with the *as-it-was-cast* choices carried on the stack object
            // (X / kicked / blight) into one CastChoicesComponent.
            run {
                val entered = updated.get<com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent>()
                var bag = entered ?: com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent()
                spellComponent.xValue?.let { bag = bag.copy(x = it) }
                if (spellComponent.wasKicked) {
                    bag = bag.withChoice(
                        com.wingedsheep.sdk.scripting.ChoiceSlot.KICKED,
                        com.wingedsheep.engine.state.components.battlefield.ChoiceValue.Flag
                    )
                }
                // Sneak (CR 702.190): durably mark the permanent so Conditions.SneakCostWasPaid
                // reads "its sneak cost was paid" for its whole life on the battlefield.
                if (spellComponent.wasSneaked) {
                    bag = bag.withChoice(
                        com.wingedsheep.sdk.scripting.ChoiceSlot.SNEAK,
                        com.wingedsheep.engine.state.components.battlefield.ChoiceValue.Flag
                    )
                }
                if (spellComponent.additionalCostBlightAmount > 0) {
                    bag = bag.withChoice(
                        com.wingedsheep.sdk.scripting.ChoiceSlot.BLIGHT_AMOUNT,
                        com.wingedsheep.engine.state.components.battlefield.ChoiceValue.NumberChoice(
                            spellComponent.additionalCostBlightAmount
                        )
                    )
                }
                if (bag.x != null || bag.chosen.isNotEmpty()) {
                    updated = updated.with(bag)
                }
            }

            // Track if this permanent was cast for its warp cost
            if (spellComponent.wasWarped) {
                updated = updated.with(WarpedComponent)
            }

            // Track if this permanent was cast for its evoke cost
            if (spellComponent.wasEvoked) {
                updated = updated.with(com.wingedsheep.engine.state.components.battlefield.EvokedComponent)
            }

            // Impending (CR 702.176a): a permanent cast for its impending cost enters with
            // N time counters. The "isn't a creature" static and the end-step removal trigger
            // both gate on impending-cost-paid AND has-time-counter, so we stamp a
            // CastForImpendingComponent marker that survives the countdown — without it, a
            // normally-cast permanent that gained a time counter from some other effect
            // would incorrectly stop being a creature.
            if (spellComponent.wasImpending) {
                val impendingTime = cardDef?.keywordAbilities
                    ?.filterIsInstance<KeywordAbility.Impending>()
                    ?.firstOrNull()?.time ?: 0
                if (impendingTime > 0) {
                    val existingCounters = updated.get<CountersComponent>() ?: CountersComponent()
                    updated = updated
                        .with(existingCounters.withAdded(CounterType.TIME, impendingTime))
                        .with(com.wingedsheep.engine.state.components.battlefield.CastForImpendingComponent)
                }
            }

            // For split-layout cards (CR 709), attach a RoomComponent recording every face's
            // unlock data and the door-state designation set. The cast face enters unlocked
            // (709.5d); other halves are locked. Cards put on the battlefield by an effect
            // other than casting (reanimation, Replenish, etc.) reach this code with
            // `spellComponent.faceIndex == null` and enter with both halves locked.
            if (cardDef != null && cardDef.layout == com.wingedsheep.sdk.model.CardLayout.SPLIT && cardDef.cardFaces.isNotEmpty()) {
                val roomFaces = cardDef.cardFaces.map { face ->
                    com.wingedsheep.engine.state.components.identity.RoomFace(
                        id = com.wingedsheep.engine.state.components.identity.RoomFaceId(face.name),
                        name = face.name,
                        manaCost = face.manaCost,
                    )
                }
                val unlockedFaceId = spellComponent.faceIndex
                    ?.let { roomFaces.getOrNull(it)?.id }
                updated = updated.with(
                    com.wingedsheep.engine.state.components.identity.RoomComponent(
                        faces = roomFaces,
                        unlocked = unlockedFaceId?.let { setOf(it) } ?: emptySet(),
                    )
                )
            }

            // Record mana colors spent to cast (for mana-spent-gated triggers)
            if (spellComponent.manaSpentWhite > 0 || spellComponent.manaSpentBlue > 0 ||
                spellComponent.manaSpentBlack > 0 || spellComponent.manaSpentRed > 0 ||
                spellComponent.manaSpentGreen > 0 || spellComponent.manaSpentColorless > 0) {
                updated = updated.with(com.wingedsheep.engine.state.components.battlefield.CastRecordComponent(
                    whiteSpent = spellComponent.manaSpentWhite,
                    blueSpent = spellComponent.manaSpentBlue,
                    blackSpent = spellComponent.manaSpentBlack,
                    redSpent = spellComponent.manaSpentRed,
                    greenSpent = spellComponent.manaSpentGreen,
                    colorlessSpent = spellComponent.manaSpentColorless
                ))
            }

            // Add continuous effects from static abilities (but not for face-down creatures)
            if (!spellComponent.castFaceDown) {
                updated = staticAbilityHandler.addContinuousEffectComponent(updated)
                updated = staticAbilityHandler.addReplacementEffectComponent(updated)
            }

            // Aura attachment: add AttachedToComponent pointing to the target
            if (auraTargetId != null) {
                updated = updated.with(
                    com.wingedsheep.engine.state.components.battlefield.AttachedToComponent(auraTargetId)
                )
            }

            updated
        }

        // Aura: add reverse AttachmentsComponent on the enchanted permanent
        if (auraTargetId != null) {
            newState = newState.updateEntity(auraTargetId) { container ->
                val existing = container.get<com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent>()
                val updatedIds = (existing?.attachedIds ?: emptyList()) + spellId
                container.with(com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent(updatedIds))
            }
        }

        // Handle "enters the battlefield tapped" replacement effect
        // Note: payLifeCost shock lands are handled in resolvePermanentSpell before this method is called.
        if (cardDef != null && !spellComponent.castFaceDown) {
            val entersTapped = cardDef.script.replacementEffects.filterIsInstance<EntersTapped>().firstOrNull()
            if (entersTapped != null && entersTapped.payLifeCost == null) {
                val shouldEnterTapped = if (entersTapped.unlessCondition != null) {
                    val context = EffectContext(
                        sourceId = spellId,
                        controllerId = controllerId,
                    )
                    !com.wingedsheep.engine.handlers.ConditionEvaluator().evaluate(
                        newState, entersTapped.unlessCondition!!, context
                    )
                } else {
                    true
                }
                if (shouldEnterTapped) {
                    newState = newState.updateEntity(spellId) { c -> c.with(TappedComponent) }
                }
            }
        }

        // Handle "enters with counters" replacement effects (before adding to battlefield)
        val counterEvents = mutableListOf<GameEvent>()

        // CR 603.2e — an Aura entering attached to its enchant target "becomes attached"; emit the
        // event so attachment triggers (Eriette, the Beguiler) fire.
        if (auraTargetId != null) {
            counterEvents.add(
                com.wingedsheep.engine.core.PermanentAttachedEvent(
                    attachmentId = spellId,
                    attachmentName = cardComponent?.name ?: "Aura",
                    attachedToId = auraTargetId,
                    controllerId = controllerId,
                )
            )
        }

        if (cardDef != null && !spellComponent.castFaceDown) {
            val totalManaSpent = spellComponent.manaSpentWhite + spellComponent.manaSpentBlue +
                spellComponent.manaSpentBlack + spellComponent.manaSpentRed +
                spellComponent.manaSpentGreen + spellComponent.manaSpentColorless
            val (counterState, events) = applyEntersWithCounters(
                newState, spellId, cardDef, controllerId, spellComponent.xValue, totalManaSpent
            )
            newState = counterState
            counterEvents.addAll(events)
        }

        // Handle planeswalker starting loyalty (Rule 306.5b)
        if (cardDef != null && !spellComponent.castFaceDown && cardDef.startingLoyalty != null) {
            val loyaltyCount = cardDef.startingLoyalty!!
            val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
                newState, spellId, CounterType.LOYALTY, loyaltyCount, placerId = controllerId
            )
            val current = newState.getEntity(spellId)?.get<CountersComponent>() ?: CountersComponent()
            newState = newState.updateEntity(spellId) { c ->
                c.with(current.withAdded(CounterType.LOYALTY, modifiedCount))
            }
        }

        // Handle Class entering the battlefield (Rule 716)
        // Add ClassLevelComponent starting at level 1
        if (cardDef != null && !spellComponent.castFaceDown && cardDef.isClass) {
            newState = newState.updateEntity(spellId) { c ->
                c.with(ClassLevelComponent(currentLevel = 1))
            }
        }

        // Handle double-faced cards entering the battlefield (Rule 712)
        // A resolving DFC spell enters with the same face that was up on the stack (Rule 712.13).
        if (cardDef != null && !spellComponent.castFaceDown && cardDef.isDoubleFaced) {
            val backFace = cardDef.backFace!!
            newState = newState.updateEntity(spellId) { c ->
                c.with(
                    com.wingedsheep.engine.state.components.identity.DoubleFacedComponent(
                        frontCardDefinitionId = cardDef.name,
                        backCardDefinitionId = backFace.name,
                        currentFace = com.wingedsheep.engine.state.components.identity.DoubleFacedComponent.Face.FRONT
                    )
                )
            }
        }

        // Handle Saga entering the battlefield (Rule 714.3a)
        // Add SagaComponent and initial lore counter (triggers chapter I detection)
        if (cardDef != null && !spellComponent.castFaceDown && cardDef.isSaga) {
            val current = newState.getEntity(spellId)?.get<CountersComponent>() ?: CountersComponent()
            // Mark chapter 1 as triggered since lore count will be 1
            val sagaComponent = SagaComponent(triggeredChapters = setOf(1))
            newState = newState.updateEntity(spellId) { c ->
                c.with(sagaComponent)
                    .with(current.withAdded(CounterType.LORE, 1))
            }
        }

        // Add to battlefield — clean up any may-play permission first (mirrors the same
        // cleanup done in resolveNonPermanentSpell before the card goes to the graveyard).
        newState = newState.removeMayPlayPermissionsForCard(spellId)
        newState = com.wingedsheep.engine.handlers.effects.BattlefieldEntry
            .place(newState, controllerId, spellId)

        // Sneak (CR 702.190b / 506.3a): a permanent spell whose sneak cost was paid enters
        // tapped and attacking the same player or planeswalker the returned unblocked creature
        // was attacking. A non-creature permanent can't attack, so it just enters tapped (506.3a).
        if (spellComponent.wasSneaked) {
            newState = newState.updateEntity(spellId) { c -> c.with(TappedComponent) }
            val projected = newState.projectedState
            // CR 506.3c: the creature only enters attacking if the carried defender is still a
            // legal attack target — an opponent still in the game, or an opponent's planeswalker
            // still on the battlefield (mirrors the defender check in AttackPhaseManager). If it's
            // no longer valid, the creature enters but is never attacking — no redirect.
            val legalDefender = spellComponent.sneakAttackDefenderId?.takeIf { d ->
                (d in newState.turnOrder && d != controllerId) ||
                    (projected.isPlaneswalker(d) &&
                        d in newState.getBattlefield() &&
                        projected.getController(d) != controllerId)
            }
            if (legalDefender != null && projected.isCreature(spellId)) {
                newState = newState.updateEntity(spellId) { c ->
                    c.with(AttackingComponent(legalDefender))
                }
            }
        }

        // For Rooms cast a half (CR 709.5d/h): the cast face's door becomes unlocked
        // on ETB. Emit a DoorUnlockedEvent so face-scoped "When you unlock this door"
        // triggers fire from the cast-time unlock too.
        val castFaceRoomComp = newState.getEntity(spellId)
            ?.get<com.wingedsheep.engine.state.components.identity.RoomComponent>()
        if (castFaceRoomComp != null && castFaceRoomComp.unlocked.size == 1) {
            val unlockedFace = castFaceRoomComp.faces.first { it.id in castFaceRoomComp.unlocked }
            counterEvents.add(
                com.wingedsheep.engine.core.DoorUnlockedEvent(
                    roomId = spellId,
                    roomName = cardComponent?.name ?: unlockedFace.name,
                    faceId = unlockedFace.id,
                    faceName = unlockedFace.name,
                    controllerId = controllerId,
                    becameFullyUnlocked = castFaceRoomComp.isFullyUnlocked
                )
            )
            if (castFaceRoomComp.isFullyUnlocked) {
                counterEvents.add(
                    com.wingedsheep.engine.core.RoomFullyUnlockedEvent(
                        roomId = spellId,
                        roomName = cardComponent?.name ?: unlockedFace.name,
                        controllerId = controllerId
                    )
                )
            }
        }

        // Warp: create delayed trigger to exile at beginning of next end step
        if (spellComponent.wasWarped) {
            val delayedTrigger = DelayedTriggeredAbility(
                id = java.util.UUID.randomUUID().toString(),
                effect = WarpExileEffect(EffectTarget.SpecificEntity(spellId)),
                fireAtStep = Step.END,
                sourceId = spellId,
                sourceName = cardComponent?.name ?: "Unknown",
                controllerId = controllerId
            )
            newState = newState.addDelayedTrigger(delayedTrigger)
        }

        // Prepared (Secrets of Strixhaven): a preparation creature enters prepared. Becoming
        // prepared creates a copy of the card's prepare spell in exile that its controller may
        // cast (paying that spell's cost); casting it unprepares the creature.
        if (cardDef != null && cardDef.layout == com.wingedsheep.sdk.model.CardLayout.PREPARE &&
            !spellComponent.castFaceDown
        ) {
            newState = makePrepared(newState, spellId, cardDef, controllerId)
        }

        return newState to counterEvents
    }

    /**
     * Make the permanent [permanentId] prepared (Secrets of Strixhaven): create a copy of the
     * card's prepare spell (`cardFaces[0]`) in [controllerId]'s exile, grant a permanent
     * cast-from-exile permission for it, and link the two via [PreparedComponent] /
     * [PreparedSpellCopyComponent]. The copy carries a [CopyOfComponent] (stack-style copy) so it
     * ceases to exist on resolution, and a [PreparedSpellCopyComponent] so the cast-from-exile
     * enumerator casts it as face 0 and the phantom-copy state-based action leaves it in exile.
     */
    private fun makePrepared(
        state: GameState,
        permanentId: EntityId,
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        controllerId: EntityId,
    ): GameState {
        val sourceCard = state.getEntity(permanentId)?.get<CardComponent>() ?: return state
        val prepareFace = cardDef.cardFaces.firstOrNull() ?: return state

        var newState = state
        val (copyId, stateWithCopy) = newState.newEntity()
        newState = stateWithCopy
        newState = newState.updateEntity(copyId) { c ->
            c.with(
                CardComponent(
                    cardDefinitionId = sourceCard.cardDefinitionId,
                    name = sourceCard.name,
                    manaCost = prepareFace.manaCost,
                    typeLine = prepareFace.typeLine,
                    oracleText = prepareFace.oracleText,
                    colors = prepareFace.manaCost.colors,
                    ownerId = controllerId,
                    spellEffect = prepareFace.script.spellEffect,
                    imageUri = sourceCard.imageUri,
                )
            ).with(
                com.wingedsheep.engine.state.components.identity.CopyOfComponent(
                    originalCardDefinitionId = sourceCard.cardDefinitionId,
                    copiedCardDefinitionId = sourceCard.cardDefinitionId,
                )
            ).with(
                com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent(sourceId = permanentId)
            )
        }
        newState = newState.addToZone(ZoneKey(controllerId, Zone.EXILE), copyId)

        val (permId, stateWithPerm) = newState.newEntity()
        newState = stateWithPerm.addMayPlayPermission(
            com.wingedsheep.engine.state.permissions.MayPlayPermission(
                id = permId,
                cardIds = setOf(copyId),
                controllerId = controllerId,
                sourceId = permanentId,
                permanent = true,
                timestamp = newState.timestamp,
            )
        )

        newState = newState.updateEntity(permanentId) { c ->
            c.with(com.wingedsheep.engine.state.components.battlefield.PreparedComponent(exileCopyId = copyId))
        }
        return newState
    }

    /**
     * Resolve a non-permanent spell - execute effects, put in graveyard.
     */
    private fun resolveNonPermanentSpell(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?,
        targets: List<ChosenTarget>,
        // Parallel to the originally-chosen targets, with `null` in slots whose target
        // was dropped by 608.2b validation. Used for [EffectContext.buildNamedTargets]
        // so BoundVariable lookups for now-illegal targets resolve to null and fizzle,
        // rather than shifting onto a later still-valid target.
        alignedTargets: List<ChosenTarget?> = targets,
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        // Execute the spell effect if present, applying text replacement if the spell
        // was modified by a text-changing effect (e.g., Artificial Evolution)
        // Use kickerSpellEffect when the spell was kicked and an alternate effect is defined.
        // Adventure / split face cast (CR 715 / 709) — when the spell was cast as a face, read
        // the face's spell effect from `cardDef.cardFaces[faceIndex].script.spellEffect`.
        val resolvedCardDef = cardComponent?.let { cardRegistry.getCard(it.name) }
        val faceSpellEffect = spellComponent.faceIndex?.let { idx ->
            resolvedCardDef?.cardFaces?.getOrNull(idx)?.script?.spellEffect
        }
        val baseSpellEffect = when {
            faceSpellEffect != null -> faceSpellEffect
            spellComponent.wasKicked && cardComponent != null ->
                resolvedCardDef?.script?.kickerSpellEffect ?: cardComponent.spellEffect
            else -> cardComponent?.spellEffect
        }
        val rawSpellEffect = baseSpellEffect
        val textReplacement = state.getEntity(spellId)?.get<TextReplacementComponent>()
        val spellEffect = if (rawSpellEffect != null && textReplacement != null) {
            rawSpellEffect.applyTextReplacement(textReplacement)
        } else {
            rawSpellEffect
        }
        if (spellEffect != null) {
            val targetRequirements = state.getEntity(spellId)?.get<TargetsComponent>()?.targetRequirements ?: emptyList()
            val context = EffectContext(
                sourceId = spellId,
                controllerId = spellComponent.casterId,
                targets = targets,
                // Position-preserving view (null in slots dropped by 608.2b) so positional
                // references — ContextTarget(n), EntityReference.Target(n), ContextPlayer(n) —
                // resolve by ORIGINAL slot and don't shift onto a later still-valid target.
                alignedTargets = alignedTargets,
                xValue = spellComponent.xValue,
                totalManaSpent = spellComponent.manaSpentWhite + spellComponent.manaSpentBlue +
                    spellComponent.manaSpentBlack + spellComponent.manaSpentRed +
                    spellComponent.manaSpentGreen + spellComponent.manaSpentColorless,
                manaSpentOnXByColor = spellComponent.manaSpentOnXByColor,
                wasKicked = spellComponent.wasKicked,
                wasBlightPaid = spellComponent.wasBlightPaid,
                wasSneaked = spellComponent.wasSneaked,
                sacrificedPermanents = spellComponent.sacrificedPermanents,
                chosenEntitySnapshots = spellComponent.chosenEntitySnapshots,
                damageDistribution = spellComponent.damageDistribution,
                chosenModes = spellComponent.chosenModes,
                modeTargetsOrdered = spellComponent.modeTargetsOrdered,
                modeTargetRequirements = spellComponent.modeTargetRequirements,
                chosenCreatureType = spellComponent.chosenCreatureType,
                exiledCardCount = spellComponent.exiledCardCount,
                additionalCostBlightAmount = spellComponent.additionalCostBlightAmount,
                castFromZone = spellComponent.castFromZone,
                pipeline = PipelineState(
                    // Use the positionally-aligned validated list so a sub-effect that
                    // references a target dropped by 608.2b through its BoundVariable id
                    // resolves to null and fizzles (CR 608.2b).
                    namedTargets = EffectContext.buildNamedTargets(targetRequirements, alignedTargets),
                    storedCollections = buildBeheldStoredCollections(spellComponent.beheldCards, resolvedCardDef)
                )
            )

            val effectResult = effectHandler.execute(newState, spellEffect, context)

            // If effect is paused awaiting a decision, we still need to move the spell
            // to graveyard/exile (it has already resolved from the stack). The decision only
            // determines how the effect completes.
            if (effectResult.isPaused) {
                val pausedIsCopy = effectResult.state.getEntity(spellId)?.has<CopyOfComponent>() == true
                if (pausedIsCopy) {
                    // Rule 112.3b — copies cease to exist when they leave the stack.
                    val pausedState = effectResult.state.removeEntity(spellId)
                    return ExecutionResult.paused(
                        pausedState,
                        effectResult.pendingDecision!!,
                        events + effectResult.events
                    )
                }

                val ownerId = cardComponent?.ownerId ?: spellComponent.casterId
                val pausedCardDef = cardComponent?.let { cardRegistry.getCard(it.name) }
                // For a cast face (Adventure / modal DFC), "Exile <name>." lives on the face's script.
                val pausedResolvedScript = spellComponent.faceIndex?.let { pausedCardDef?.cardFaces?.getOrNull(it)?.script }
                    ?: pausedCardDef?.script
                val pausedSelfExile = pausedResolvedScript?.selfExileOnResolve == true
                // Flashback (printed or granted — Archmage's Newt) or Harmonize (printed or granted
                // — Songcrafter Mage): a graveyard cast exiles on resolution instead of returning
                // to the graveyard.
                val pausedFlashbackExile = spellComponent.castFromZone == Zone.GRAVEYARD &&
                    (FlashbackGrants.effectiveFlashback(state, spellId, pausedCardDef) != null ||
                        HarmonizeGrants.effectiveHarmonize(state, spellId, pausedCardDef) != null)
                val pausedExileAfterResolveComp = effectResult.state.getEntity(spellId)?.get<ExileAfterResolveComponent>()
                val pausedExileAfterResolve = pausedExileAfterResolveComp != null
                val pausedAdventureFaceExile = pausedCardDef?.layout == com.wingedsheep.sdk.model.CardLayout.ADVENTURE &&
                    spellComponent.faceIndex != null
                val pausedOmenFaceShuffle = pausedCardDef?.layout == com.wingedsheep.sdk.model.CardLayout.OMEN &&
                    spellComponent.faceIndex != null
                val pausedIntended = when {
                    pausedSelfExile || pausedFlashbackExile || pausedExileAfterResolve || pausedAdventureFaceExile -> Zone.EXILE
                    pausedOmenFaceShuffle -> Zone.LIBRARY
                    else -> Zone.GRAVEYARD
                }

                // Apply RedirectZoneChange replacement effects (e.g., Festival of Embers).
                val pausedRedirect = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.checkZoneChangeRedirect(
                    effectResult.state, spellId, Zone.STACK, pausedIntended
                )
                val pausedDestZone = pausedRedirect.destinationZone
                val pausedDestZoneKey = ZoneKey(ownerId, pausedDestZone)

                // Move spell to graveyard/exile even though effect is paused
                var pausedState = effectResult.state.updateEntity(spellId) { c ->
                    c.without<SpellOnStackComponent>().without<TargetsComponent>()
                }
                pausedState = pausedState.addToZone(pausedDestZoneKey, spellId)

                // Paradigm: tag the just-exiled spell even when its effect paused mid-resolution.
                if (pausedDestZone == Zone.EXILE && pausedResolvedScript?.paradigm == true) {
                    pausedState = pausedState.updateEntity(spellId) { c ->
                        c.with(com.wingedsheep.engine.state.components.battlefield.ParadigmComponent)
                    }
                }

                // CR 715.3d — Adventure exiled by its own resolution: re-grant cast-from-exile.
                if (pausedAdventureFaceExile && pausedDestZone == Zone.EXILE) {
                    val (permId, stateWithPerm) = pausedState.newEntity()
                    pausedState = stateWithPerm.addMayPlayPermission(
                        com.wingedsheep.engine.state.permissions.MayPlayPermission(
                            id = permId,
                            cardIds = setOf(spellId),
                            controllerId = spellComponent.casterId,
                            permanent = true,
                            timestamp = state.timestamp,
                        )
                    )
                }

                val pausedCounterEvents = mutableListOf<GameEvent>()
                if (pausedDestZone == Zone.EXILE && pausedExileAfterResolveComp != null && pausedExileAfterResolveComp.withCounters.isNotEmpty()) {
                    pausedState = applyExileCounters(pausedState, spellId, pausedExileAfterResolveComp.withCounters, pausedCounterEvents)
                }

                // Omen (Tarkir: Dragonstorm): shuffle the just-added card into its owner's library.
                if (pausedOmenFaceShuffle && pausedDestZone == Zone.LIBRARY) {
                    pausedState = shuffleOwnerLibrary(pausedState, ownerId)
                    pausedCounterEvents.add(LibraryShuffledEvent(ownerId))
                }

                pausedRedirect.additionalEffect?.let { extra ->
                    pausedState = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.applyReplacementAdditionalEffect(
                        pausedState, extra, pausedRedirect.effectControllerId, spellId
                    )
                }

                // Include the zone change event along with effect events
                val allEvents = events + effectResult.events + ZoneChangeEvent(
                    spellId,
                    cardComponent?.name ?: "Unknown",
                    null,
                    pausedDestZone,
                    ownerId
                ) + pausedCounterEvents

                return ExecutionResult.paused(
                    pausedState,
                    effectResult.pendingDecision!!,
                    allEvents
                )
            }

            // Always apply state changes from effect execution, even on partial
            // failure. Per MTG rules, when a spell resolves, you do as much as
            // possible. Partial state changes (e.g., first target destroyed but
            // second target missing) should be preserved.
            newState = effectResult.newState
            events.addAll(effectResult.events)
        }

        // Rule 112.3b: a copy of a spell ceases to exist when it leaves the stack —
        // it does not go to a graveyard or exile.
        val isCopy = newState.getEntity(spellId)?.has<CopyOfComponent>() == true
        if (isCopy) {
            newState = newState.removeEntity(spellId)
            return ExecutionResult.success(newState, events)
        }

        // Move to graveyard (or exile if selfExileOnResolve, flashback, or ExileAfterResolveComponent)
        val ownerId = cardComponent?.ownerId ?: spellComponent.casterId
        val cardDef = cardComponent?.let { cardRegistry.getCard(it.name) }
        // For a cast face (Adventure / modal DFC), "Exile <name>." lives on the face's script.
        val resolvedScript = spellComponent.faceIndex?.let { cardDef?.cardFaces?.getOrNull(it)?.script }
            ?: cardDef?.script
        val selfExile = resolvedScript?.selfExileOnResolve == true
        // Flashback (printed or granted — Archmage's Newt) or Harmonize (printed or granted —
        // Songcrafter Mage): a graveyard cast exiles on resolution instead of returning to the
        // graveyard.
        val flashbackExile = spellComponent.castFromZone == Zone.GRAVEYARD &&
            (FlashbackGrants.effectiveFlashback(state, spellId, cardDef) != null ||
                HarmonizeGrants.effectiveHarmonize(state, spellId, cardDef) != null)
        val exileAfterResolveComp = newState.getEntity(spellId)?.get<ExileAfterResolveComponent>()
        val exileAfterResolve = exileAfterResolveComp != null
        // Adventure face (CR 715.3d): when an Adventure resolves, exile it instead of putting
        // it in its owner's graveyard, and grant the caster permission to cast it as the
        // creature spell while it remains exiled.
        val adventureFaceExile = cardDef?.layout == com.wingedsheep.sdk.model.CardLayout.ADVENTURE &&
            spellComponent.faceIndex != null
        // Omen face (Tarkir: Dragonstorm): when an Omen resolves, shuffle it into its owner's
        // library instead of putting it in the graveyard. No cast-from-exile linkage.
        val omenFaceShuffle = cardDef?.layout == com.wingedsheep.sdk.model.CardLayout.OMEN &&
            spellComponent.faceIndex != null
        val intendedDestination = when {
            selfExile || flashbackExile || exileAfterResolve || adventureFaceExile -> Zone.EXILE
            omenFaceShuffle -> Zone.LIBRARY
            else -> Zone.GRAVEYARD
        }

        // Apply RedirectZoneChange replacement effects (e.g., Festival of Embers
        // exiles cards that would go to your graveyard from anywhere).
        val redirect = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.checkZoneChangeRedirect(
            newState, spellId, Zone.STACK, intendedDestination
        )
        val destinationZone = redirect.destinationZone
        val destZoneKey = ZoneKey(ownerId, destinationZone)

        newState = newState.updateEntity(spellId) { c ->
            c.without<SpellOnStackComponent>()
                .without<TargetsComponent>()
                .without<com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent>()
                .without<com.wingedsheep.engine.state.components.identity.PlayWithCostIncreaseComponent>()
                .without<ExileAfterResolveComponent>()
        }
        newState = newState.removeMayPlayPermissionsForCard(spellId)
        newState = newState.addToZone(destZoneKey, spellId)

        // Paradigm (Secrets of Strixhaven): tag the just-exiled spell so the engine synthesizes its
        // recurring precombat-main free-recast ability (Paradigm.recastAbility). The marker is the
        // gate — a Lesson exiled by any other path carries no marker and so never recurs.
        if (destinationZone == Zone.EXILE && resolvedScript?.paradigm == true) {
            newState = newState.updateEntity(spellId) { c ->
                c.with(com.wingedsheep.engine.state.components.battlefield.ParadigmComponent)
            }
        }

        // CR 715.3d — an Adventure card exiled by its own resolution may be cast as the creature
        // by the spell's controller while it remains in exile. Re-add the permission after
        // the prior removeMayPlayPermissionsForCard so the cast-from-exile enumerator picks
        // it up on the next priority pass.
        if (adventureFaceExile && destinationZone == Zone.EXILE) {
            val (permId, stateWithPerm) = newState.newEntity()
            newState = stateWithPerm.addMayPlayPermission(
                com.wingedsheep.engine.state.permissions.MayPlayPermission(
                    id = permId,
                    cardIds = setOf(spellId),
                    controllerId = spellComponent.casterId,
                    permanent = true,
                    timestamp = state.timestamp,
                )
            )
        }

        // Omen (Tarkir: Dragonstorm): the card was just added to the bottom of its owner's
        // library above — now shuffle that library and announce it.
        if (omenFaceShuffle && destinationZone == Zone.LIBRARY) {
            newState = shuffleOwnerLibrary(newState, ownerId)
            events.add(LibraryShuffledEvent(ownerId))
        }

        // Add counters granted by ExileAfterResolveComponent (e.g., Goliath Daydreamer's dream counter).
        if (destinationZone == Zone.EXILE && exileAfterResolveComp != null && exileAfterResolveComp.withCounters.isNotEmpty()) {
            newState = applyExileCounters(newState, spellId, exileAfterResolveComp.withCounters, events)
        }

        // Make the exiled card plotted (Lilah, Undefeated Slickshot): "exile that spell instead of
        // putting it into your graveyard as it resolves. If you do, it becomes plotted."
        if (destinationZone == Zone.EXILE && exileAfterResolveComp?.makePlotted == true) {
            newState = applyPlottedToExiledCard(newState, spellId, ownerId, cardComponent?.name ?: "Unknown", events)
        }

        // Link the exiled spell back to the source permanent (Goliath Daydreamer)
        // so the UI can display it tethered under the source and so the attack-trigger
        // free-cast ability can find it via the linked-exile pile.
        if (destinationZone == Zone.EXILE && exileAfterResolveComp?.linkedSourceId != null) {
            val sourceId = exileAfterResolveComp.linkedSourceId
            if (newState.getEntity(sourceId) != null) {
                newState = newState.updateEntity(sourceId) { c ->
                    val existing = c.get<com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent>()
                    val updated = (existing?.exiledIds ?: emptyList()) + spellId
                    c.with(com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent(updated))
                }
            }
        }

        redirect.additionalEffect?.let { extra ->
            newState = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.applyReplacementAdditionalEffect(
                newState, extra, redirect.effectControllerId, spellId
            )
        }

        events.add(
            ZoneChangeEvent(
                spellId,
                cardComponent?.name ?: "Unknown",
                null,
                destinationZone,
                ownerId
            )
        )

        return ExecutionResult.success(newState, events)
    }

    /**
     * Shuffle [ownerId]'s library after an Omen spell has been added to it on resolution
     * (Tarkir: Dragonstorm — "then shuffle this card into its owner's library"). Mirrors
     * [com.wingedsheep.engine.handlers.effects.library.ShuffleLibraryExecutor]: clears any
     * known top-of-library positions before shuffling, then advances the deterministic RNG.
     * The caller is responsible for emitting the [LibraryShuffledEvent].
     */
    private fun shuffleOwnerLibrary(state: GameState, ownerId: EntityId): GameState {
        val libraryZone = ZoneKey(ownerId, Zone.LIBRARY)
        val cleared = com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils
            .clearLibraryReveals(state, ownerId)
        val (library, advanced) = cleared.nextRandom { shuffle(cleared.getZone(libraryZone)) }
        return advanced.copy(zones = advanced.zones + (libraryZone to library))
    }

    /**
     * Add counters to a card that was just exiled because of ExileAfterResolveComponent.
     * Used by Goliath Daydreamer to put a dream counter on cast spells as they're exiled.
     */
    private fun applyExileCounters(
        state: GameState,
        cardId: EntityId,
        counters: List<com.wingedsheep.sdk.core.CounterType>,
        events: MutableList<GameEvent>
    ): GameState {
        val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: ""
        var updated = state
        for (counterType in counters) {
            updated = updated.updateEntity(cardId) { c ->
                val current = c.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
                    ?: com.wingedsheep.engine.state.components.battlefield.CountersComponent()
                c.with(current.withAdded(counterType, 1))
            }
            events.add(CountersAddedEvent(cardId, counterType.name, 1, cardName))
        }
        return updated
    }

    /**
     * Spell fizzles because all targets are invalid.
     */
    private fun fizzleSpell(
        state: GameState,
        spellId: EntityId,
        cardComponent: CardComponent?,
        spellComponent: SpellOnStackComponent
    ): ExecutionResult {
        // Rule 112.3b — a copy that fizzles ceases to exist rather than moving to graveyard/exile.
        val isCopy = state.getEntity(spellId)?.has<CopyOfComponent>() == true
        if (isCopy) {
            val newState = state.removeEntity(spellId)
            return ExecutionResult.success(
                newState,
                listOf(
                    SpellFizzledEvent(spellId, cardComponent?.name ?: "Unknown", "All targets are invalid")
                )
            )
        }

        val ownerId = cardComponent?.ownerId ?: spellComponent.casterId
        val cardDef = cardComponent?.let { cardRegistry.getCard(it.name) }
        // Flashback (printed or granted — Archmage's Newt) or Harmonize (printed or granted —
        // Songcrafter Mage): a graveyard cast exiles on resolution instead of returning to the
        // graveyard.
        val flashbackExile = spellComponent.castFromZone == Zone.GRAVEYARD &&
            (FlashbackGrants.effectiveFlashback(state, spellId, cardDef) != null ||
                HarmonizeGrants.effectiveHarmonize(state, spellId, cardDef) != null)
        val exileAfterResolveComp = state.getEntity(spellId)?.get<ExileAfterResolveComponent>()
        // Goliath Daydreamer-style components only exile on actual resolution; if the spell
        // fizzles or is countered they go to graveyard normally.
        val exileAfterResolve = exileAfterResolveComp != null && !exileAfterResolveComp.onlyIfResolved
        val destZone = if (flashbackExile || exileAfterResolve) Zone.EXILE else Zone.GRAVEYARD
        val destZoneKey = ZoneKey(ownerId, destZone)

        var newState = state.updateEntity(spellId) { c ->
            c.without<SpellOnStackComponent>().without<TargetsComponent>()
        }
        newState = newState.addToZone(destZoneKey, spellId)

        return ExecutionResult.success(
            newState,
            listOf(
                SpellFizzledEvent(spellId, cardComponent?.name ?: "Unknown", "All targets are invalid"),
                ZoneChangeEvent(
                    spellId,
                    cardComponent?.name ?: "Unknown",
                    null,
                    destZone,
                    ownerId
                )
            )
        )
    }

    /**
     * Resolve a triggered ability.
     */
    private fun resolveTriggeredAbility(
        state: GameState,
        abilityId: EntityId,
        container: ComponentContainer
    ): ExecutionResult {
        val abilityComponent = container.get<TriggeredAbilityOnStackComponent>()!!
        val targetsComponent = container.get<TargetsComponent>()

        // Validate targets (including protection check - Rule 702.16)
        val sourceCard = state.getEntity(abilityComponent.sourceId)?.get<CardComponent>()
        val sourceColors = sourceCard?.colors ?: emptySet()
        val sourceSubtypes = sourceCard?.typeLine?.subtypes?.map { it.value }?.toSet() ?: emptySet()
        if (targetsComponent != null && targetsComponent.targets.isNotEmpty()) {
            val validTargets = validateTargets(
                state, targetsComponent.targets, sourceColors, sourceSubtypes,
                abilityComponent.controllerId, targetsComponent.targetRequirements,
                sourceId = abilityComponent.sourceId,
                targetingSourceType = TargetingSourceType.ABILITY,
                xValue = abilityComponent.xValue
            )
            if (validTargets.isEmpty()) {
                // Fizzle - remove ability entity
                val newState = state.removeEntity(abilityId)
                return ExecutionResult.success(
                    newState,
                    listOf(
                        AbilityFizzledEvent(
                            abilityComponent.sourceId,
                            abilityComponent.description,
                            "All targets are invalid"
                        )
                    )
                )
            }
        }

        // Execute the effect
        val resolvedTargets2 = targetsComponent?.targets ?: emptyList()
        val targetReqs = targetsComponent?.targetRequirements ?: emptyList()
        val context = EffectContext(
            sourceId = abilityComponent.sourceId,
            controllerId = abilityComponent.controllerId,
            abilityIdentity = abilityComponent.abilityIdentity,
            targets = resolvedTargets2,
            triggerDamageAmount = abilityComponent.triggerDamageAmount,
            triggerCounterCount = abilityComponent.triggerCounterCount,
            triggerTotalCounterCount = abilityComponent.triggerTotalCounterCount,
            triggerLastKnownCounters = abilityComponent.triggerLastKnownCounters,
            triggerLastKnownDamageDealtByPlayers = abilityComponent.triggerLastKnownDamageDealtByPlayers,
            triggerLastKnownBlockingOrBlockedByIds = abilityComponent.triggerLastKnownBlockingOrBlockedByIds,
            triggeringEntityId = abilityComponent.triggeringEntityId,
            triggeringPlayerId = abilityComponent.triggeringPlayerId,
            targetingSourceEntityId = abilityComponent.targetingSourceEntityId,
            triggerLastKnownPower = abilityComponent.lastKnownPower,
            triggerLastKnownToughness = abilityComponent.lastKnownToughness,
            enchantedCreatureLastKnownPower = abilityComponent.enchantedCreatureLastKnownPower,
            triggerModesChosenCount = abilityComponent.triggerModesChosenCount,
            triggerScryCount = abilityComponent.triggerScryCount,
            triggerExcessDamageAmount = abilityComponent.triggerExcessDamageAmount,
            triggerRecipientToughness = abilityComponent.triggerRecipientToughness,
            triggerManaSpentOnTriggeringSpell = abilityComponent.triggerManaSpentOnTriggeringSpell,
            triggerColorsSpentOnTriggeringSpell = abilityComponent.triggerColorsSpentOnTriggeringSpell,
            triggerManaValueOfTriggeringSpell = abilityComponent.triggerManaValueOfTriggeringSpell,
            triggerXValueOfTriggeringSpell = abilityComponent.triggerXValueOfTriggeringSpell,
            xValue = abilityComponent.xValue,
            damageDistribution = abilityComponent.damageDistribution,
            chosenModes = abilityComponent.chosenModes,
            modeTargetsOrdered = abilityComponent.modeTargetsOrdered,
            modeTargetRequirements = abilityComponent.modeTargetRequirements,
            pipeline = PipelineState(
                namedTargets = EffectContext.buildNamedTargets(targetReqs, resolvedTargets2),
                // Expose a batch trigger's captured permanents (the matching members of a
                // PermanentsEnteredEvent batch) so a ForEachInCollectionEffect payoff can iterate
                // them — "for each of them, create a tapped copy of it" (Kambal). The copy executor
                // reads each entity at resolution, so any that left the battlefield meanwhile no-op.
                storedCollections = if (abilityComponent.capturedEntityIds.isNotEmpty()) {
                    mapOf(PipelineState.TRIGGER_CAPTURED_COLLECTION to abilityComponent.capturedEntityIds)
                } else emptyMap()
            )
        )

        val effectResult = effectHandler.execute(state, abilityComponent.effect, context)

        // If effect is paused awaiting a decision, return paused state
        // The ability entity stays removed (it's off the stack), but the decision must resolve
        if (effectResult.isPaused) {
            val pausedState = effectResult.state.removeEntity(abilityId)
            return ExecutionResult.paused(
                pausedState,
                effectResult.pendingDecision!!,
                effectResult.events
            )
        }

        var newState = effectResult.newState

        // Remove the ability entity
        newState = newState.removeEntity(abilityId)

        return ExecutionResult.success(
            newState,
            effectResult.events + AbilityResolvedEvent(
                abilityComponent.sourceId,
                abilityComponent.description
            )
        )
    }

    /**
     * Resolve an activated ability.
     */
    private fun resolveActivatedAbility(
        state: GameState,
        abilityId: EntityId,
        container: ComponentContainer
    ): ExecutionResult {
        val abilityComponent = container.get<ActivatedAbilityOnStackComponent>()!!
        val targetsComponent = container.get<TargetsComponent>()

        // Validate targets (including protection check - Rule 702.16)
        val sourceCard = state.getEntity(abilityComponent.sourceId)?.get<CardComponent>()
        val sourceColors = sourceCard?.colors ?: emptySet()
        val sourceSubtypes = sourceCard?.typeLine?.subtypes?.map { it.value }?.toSet() ?: emptySet()
        if (targetsComponent != null && targetsComponent.targets.isNotEmpty()) {
            val validTargets = validateTargets(
                state, targetsComponent.targets, sourceColors, sourceSubtypes,
                abilityComponent.controllerId, targetsComponent.targetRequirements,
                sourceId = abilityComponent.sourceId,
                xValue = abilityComponent.xValue
            )
            if (validTargets.isEmpty()) {
                val newState = state.removeEntity(abilityId)
                return ExecutionResult.success(
                    newState,
                    listOf(
                        AbilityFizzledEvent(
                            abilityComponent.sourceId,
                            abilityComponent.sourceName,
                            "All targets are invalid"
                        )
                    )
                )
            }
        }

        // Execute the effect
        val activatedTargets = targetsComponent?.targets ?: emptyList()
        val activatedReqs = targetsComponent?.targetRequirements ?: emptyList()
        val context = EffectContext(
            sourceId = abilityComponent.sourceId,
            controllerId = abilityComponent.controllerId,
            abilityIdentity = abilityComponent.abilityIdentity,
            targets = activatedTargets,
            sacrificedPermanents = abilityComponent.sacrificedPermanents,
            xValue = abilityComponent.xValue,
            tappedPermanents = abilityComponent.tappedPermanents,
            tappedPermanentSnapshots = abilityComponent.tappedPermanentSnapshots,
            pipeline = PipelineState(namedTargets = EffectContext.buildNamedTargets(activatedReqs, activatedTargets))
        )

        val effectResult = effectHandler.execute(state, abilityComponent.effect, context)

        // If effect is paused awaiting a decision, return paused state
        // The ability entity stays removed (it's off the stack), but the decision must resolve
        if (effectResult.isPaused) {
            val pausedState = effectResult.state.removeEntity(abilityId)
            return ExecutionResult.paused(
                pausedState,
                effectResult.pendingDecision!!,
                effectResult.events
            )
        }

        var newState = effectResult.newState

        // Remove the ability entity
        newState = newState.removeEntity(abilityId)

        return ExecutionResult.success(
            newState,
            effectResult.events + AbilityResolvedEvent(
                abilityComponent.sourceId,
                abilityComponent.sourceName
            )
        )
    }

    // =========================================================================
    // Enters With Counters
    // =========================================================================

    private val dynamicAmountEvaluator = DynamicAmountEvaluator()
    private val conditionEvaluator = com.wingedsheep.engine.handlers.ConditionEvaluator()

    /**
     * Apply "enters with counters" replacement effects to a permanent.
     * Handles both fixed count (EntersWithCounters) and dynamic count (EntersWithDynamicCounters).
     * Also checks other battlefield permanents for replacement effects that modify entering creatures
     * (e.g., Gev, Scaled Scorch: "Other creatures you control enter with additional +1/+1 counters").
     */
    internal fun applyEntersWithCounters(
        state: GameState,
        entityId: EntityId,
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        controllerId: EntityId,
        xValue: Int? = null,
        totalManaSpent: Int = 0
    ): Pair<GameState, List<GameEvent>> {
        var newState = state
        val events = mutableListOf<GameEvent>()
        val entityName = newState.getEntity(entityId)?.get<CardComponent>()?.name ?: ""
        // Apply the entering creature's own replacement effects
        for (effect in cardDef.script.replacementEffects) {
            when (effect) {
                is EntersWithCounters -> {
                    if (effect.condition != null) {
                        val condContext = EffectContext(
                            sourceId = entityId,
                            controllerId = controllerId,
                        )
                        if (!conditionEvaluator.evaluate(newState, effect.condition!!, condContext)) continue
                    }
                    val counterType = resolveCounterType(effect.counterType)
                    val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
                        newState, entityId, counterType, effect.count, placerId = controllerId
                    )
                    val current = newState.getEntity(entityId)?.get<CountersComponent>() ?: CountersComponent()
                    newState = newState.updateEntity(entityId) { c ->
                        c.with(current.withAdded(counterType, modifiedCount))
                    }
                    val (afterMark, firstThisTurn) = DamageUtils.recordCounterPlacement(newState, entityId)
                    newState = afterMark
                    events.add(CountersAddedEvent(entityId, effect.counterType.description, modifiedCount, entityName, firstThisTurn))
                }
                is EntersWithDynamicCounters -> {
                    // Skip "other only" effects when applying to self (e.g., Gev)
                    if (effect.otherOnly) continue
                    val counterType = resolveCounterType(effect.counterType)
                    val context = EffectContext(
                        sourceId = entityId,
                        controllerId = controllerId,
                        xValue = xValue,
                        totalManaSpent = totalManaSpent
                    )
                    val count = dynamicAmountEvaluator.evaluate(newState, effect.count, context)
                    if (count > 0) {
                        val modifiedCount = ReplacementEffectUtils.applyCounterPlacementModifiers(
                            newState, entityId, counterType, count, placerId = controllerId
                        )
                        val current = newState.getEntity(entityId)?.get<CountersComponent>() ?: CountersComponent()
                        newState = newState.updateEntity(entityId) { c ->
                            c.with(current.withAdded(counterType, modifiedCount))
                        }
                        val (afterMark, firstThisTurn) = DamageUtils.recordCounterPlacement(newState, entityId)
                        newState = afterMark
                        events.add(CountersAddedEvent(entityId, effect.counterType.description, modifiedCount, entityName, firstThisTurn))
                    }
                }
                else -> { /* Other replacement effects handled elsewhere */ }
            }
        }

        // Apply "enters with counters" replacement effects from other battlefield permanents
        // (e.g., Gev: "Other creatures you control enter with additional +1/+1 counters")
        val (globalState, globalEvents) = EntersWithCountersHelper.applyGlobalEntersWithCounters(
            newState, entityId, controllerId
        )
        newState = globalState
        events.addAll(globalEvents)

        return newState to events
    }

    private fun resolveCounterType(filter: CounterTypeFilter): CounterType =
        EntersWithCountersHelper.resolveCounterType(filter)

    // =========================================================================
    // Countering
    // =========================================================================

    /**
     * Counter a spell on the stack.
     */
    fun counterSpell(state: GameState, spellId: EntityId): ExecutionResult {
        if (spellId !in state.stack) {
            return ExecutionResult.error(state, "Spell not on stack: $spellId")
        }

        val container = state.getEntity(spellId)
            ?: return ExecutionResult.error(state, "Spell not found: $spellId")

        val cardComponent = container.get<CardComponent>()

        // Check if the spell can't be countered (tag component)
        if (container.has<CantBeCounteredComponent>()) {
            return ExecutionResult.success(state)
        }

        // Check if any permanent on the battlefield grants "can't be countered" to this spell
        if (isGrantedCantBeCountered(state, spellId)) {
            return ExecutionResult.success(state)
        }

        val spellComponent = container.get<SpellOnStackComponent>()
        val ownerId = cardComponent?.ownerId
            ?: spellComponent?.casterId
            ?: return ExecutionResult.error(state, "Cannot determine spell owner")

        // Remove from stack
        var newState = state.removeFromStack(spellId)

        // Put in graveyard (or exile if ExileAfterResolveComponent is present)
        // Goliath Daydreamer-style components only exile on actual resolution; if the spell
        // is countered they go to graveyard normally.
        val exileComp = container.get<ExileAfterResolveComponent>()
        val exileAfterResolve = exileComp != null && !exileComp.onlyIfResolved
        val destZone = if (exileAfterResolve) Zone.EXILE else Zone.GRAVEYARD
        val destZoneKey = ZoneKey(ownerId, destZone)
        newState = newState.addToZone(destZoneKey, spellId)

        // Remove stack components
        newState = newState.updateEntity(spellId) { c ->
            c.without<SpellOnStackComponent>().without<TargetsComponent>()
        }

        return ExecutionResult.success(
            newState,
            listOf(
                SpellCounteredEvent(spellId, cardComponent?.name ?: "Unknown"),
                ZoneChangeEvent(
                    spellId,
                    cardComponent?.name ?: "Unknown",
                    null,
                    destZone,
                    ownerId
                )
            )
        )
    }

    /**
     * Counter a spell on the stack and exile it instead of putting it into
     * its owner's graveyard. If the spell can't be countered, nothing happens.
     *
     * @param grantFreeCast If true, the controller of this effect may cast the
     *   exiled card without paying its mana cost for as long as it remains exiled.
     * @param controllerId The player who gains permission to cast the exiled card.
     * @return ExecutionResult with a boolean flag indicating if the spell was actually countered.
     */
    fun counterSpellToExile(
        state: GameState,
        spellId: EntityId,
        grantFreeCast: Boolean,
        controllerId: EntityId
    ): ExecutionResult {
        if (spellId !in state.stack) {
            return ExecutionResult.error(state, "Spell not on stack: $spellId")
        }

        val container = state.getEntity(spellId)
            ?: return ExecutionResult.error(state, "Spell not found: $spellId")

        val cardComponent = container.get<CardComponent>()

        // Check if the spell can't be countered
        if (container.has<CantBeCounteredComponent>() || isGrantedCantBeCountered(state, spellId)) {
            return ExecutionResult.success(state)
        }

        val spellComponent = container.get<SpellOnStackComponent>()
        val ownerId = cardComponent?.ownerId
            ?: spellComponent?.casterId
            ?: return ExecutionResult.error(state, "Cannot determine spell owner")

        // Remove from stack
        var newState = state.removeFromStack(spellId)

        // Put in exile (instead of graveyard)
        val exileZone = ZoneKey(ownerId, Zone.EXILE)
        newState = newState.addToZone(exileZone, spellId)

        // Remove stack components and optionally grant free cast
        newState = newState.updateEntity(spellId) { c ->
            var updated = c.without<SpellOnStackComponent>().without<TargetsComponent>()
            if (grantFreeCast) {
                updated = updated
                    .with(PlayWithoutPayingCostComponent(controllerId = controllerId, permanent = true))
            }
            updated
        }
        if (grantFreeCast) {
            val (permId, stateWithPerm) = newState.newEntity()
            newState = stateWithPerm.addMayPlayPermission(
                com.wingedsheep.engine.state.permissions.MayPlayPermission(
                    id = permId,
                    cardIds = setOf(spellId),
                    controllerId = controllerId,
                    permanent = true,
                    timestamp = state.timestamp,
                )
            )
        }

        return ExecutionResult.success(
            newState,
            listOf(
                SpellCounteredEvent(spellId, cardComponent?.name ?: "Unknown"),
                ZoneChangeEvent(
                    spellId,
                    cardComponent?.name ?: "Unknown",
                    null,
                    Zone.EXILE,
                    ownerId
                )
            )
        )
    }

    /**
     * Exile a spell on the stack (CR 718 "exile target spell" — Aven Interrupter), optionally
     * making it *plotted* for its owner.
     *
     * Unlike [counterSpellToExile] this is **not** a counter: it ignores can't-be-countered
     * (the spell is exiled regardless — Aven Interrupter's ruling: "Spells that can't be
     * countered can still be exiled"), and it emits no [SpellCounteredEvent] (so "whenever a
     * spell is countered" triggers don't fire). The spell still ceases to resolve because it
     * leaves the stack. A [ZoneChangeEvent] from [Zone.STACK] to [Zone.EXILE] is emitted.
     *
     * When [makePlotted] is true the exiled card gets the plotted designation and a permanent
     * free-cast-on-a-later-turn permission gated by [SourcePlottedOnPriorTurn], granted to the
     * card's **owner** (CR 718.2 / the reminder text: "Its owner may cast it as a sorcery on a
     * later turn without paying its mana cost"), and a [CardPlottedEvent] is emitted.
     */
    fun exileSpell(
        state: GameState,
        spellId: EntityId,
        makePlotted: Boolean
    ): ExecutionResult {
        if (spellId !in state.stack) {
            return ExecutionResult.error(state, "Spell not on stack: $spellId")
        }
        val container = state.getEntity(spellId)
            ?: return ExecutionResult.error(state, "Spell not found: $spellId")
        val cardComponent = container.get<CardComponent>()
        val spellComponent = container.get<SpellOnStackComponent>()
        val ownerId = cardComponent?.ownerId
            ?: spellComponent?.casterId
            ?: return ExecutionResult.error(state, "Cannot determine spell owner")

        // Remove from the stack and put the card into its owner's exile.
        var newState = state.removeFromStack(spellId)
        val exileZone = ZoneKey(ownerId, Zone.EXILE)
        newState = newState.addToZone(exileZone, spellId)
        newState = newState.updateEntity(spellId) { c ->
            c.without<SpellOnStackComponent>().without<TargetsComponent>()
        }

        val events = mutableListOf<GameEvent>(
            ZoneChangeEvent(spellId, cardComponent?.name ?: "Unknown", Zone.STACK, Zone.EXILE, ownerId)
        )

        if (makePlotted) {
            newState = applyPlottedToExiledCard(newState, spellId, ownerId, cardComponent?.name ?: "Unknown", events)
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Make a card that already sits in [ownerId]'s exile *plotted* (CR 718): tag it with
     * [PlottedComponent] + [PlayWithoutPayingCostComponent], grant a permanent may-play
     * permission gated on [SourcePlottedOnPriorTurn] (a plotted card can't be cast the turn it
     * was plotted), and emit [CardPlottedEvent]. Shared by [ExileTargetSpellEffect]'s
     * `makePlotted` path and the [ExileAfterResolveComponent].`makePlotted` self-cast path
     * (Lilah, Undefeated Slickshot).
     */
    private fun applyPlottedToExiledCard(
        state: GameState,
        cardId: EntityId,
        ownerId: EntityId,
        cardName: String,
        events: MutableList<GameEvent>,
    ): GameState {
        val turnPlotted = state.turnNumber
        var newState = state.updateEntity(cardId) { c ->
            c.with(PlottedComponent(controllerId = ownerId, turnPlotted = turnPlotted))
                .with(PlayWithoutPayingCostComponent(controllerId = ownerId, permanent = true))
        }
        val (permId, stateWithPerm) = newState.newEntity()
        newState = stateWithPerm.addMayPlayPermission(
            MayPlayPermission(
                id = permId,
                cardIds = setOf(cardId),
                controllerId = ownerId,
                sourceId = cardId,
                condition = SourcePlottedOnPriorTurn,
                permanent = true,
                timestamp = newState.timestamp,
            )
        )
        events.add(CardPlottedEvent(ownerId, cardId, cardName))
        return newState
    }

    /**
     * Counter an activated or triggered ability on the stack.
     * Unlike countering a spell, the ability is simply removed from the stack
     * without going to any zone (abilities are not cards).
     */
    fun counterAbility(state: GameState, abilityId: EntityId): ExecutionResult {
        if (abilityId !in state.stack) {
            return ExecutionResult.error(state, "Ability not on stack: $abilityId")
        }

        val container = state.getEntity(abilityId)
            ?: return ExecutionResult.error(state, "Ability not found: $abilityId")

        val description = container.get<TriggeredAbilityOnStackComponent>()?.description
            ?: container.get<ActivatedAbilityOnStackComponent>()?.let { "${it.sourceName}'s ability" }
            ?: "Unknown ability"

        // Remove from stack — abilities don't go to any zone
        val newState = state.removeFromStack(abilityId)

        return ExecutionResult.success(
            newState,
            listOf(AbilityCounteredEvent(abilityId, description))
        )
    }

    // =========================================================================
    // Target Validation
    // =========================================================================

    /**
     * Validate targets and return only valid ones.
     *
     * Checks zone existence, protection (Rule 702.16), and target filter matching
     * (Rule 608.2b — targets must still be legal when the spell/ability resolves).
     */
    private fun validateTargets(
        state: GameState,
        targets: List<ChosenTarget>,
        sourceColors: Set<Color> = emptySet(),
        sourceSubtypes: Set<String> = emptySet(),
        controllerId: EntityId,
        targetRequirements: List<TargetRequirement> = emptyList(),
        sourceId: EntityId? = null,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY,
        xValue: Int? = null
    ): List<ChosenTarget> {
        // Always project state for shroud/hexproof checks (Rule 702.18, 702.11)
        val projected = state.projectedState
        val predicateContext = PredicateContext(controllerId = controllerId, sourceId = sourceId, xValue = xValue)

        return targets.filterIndexed { index, target ->
            when (target) {
                is ChosenTarget.Player -> {
                    // Player is valid if they exist and haven't lost...
                    if (!state.hasEntity(target.playerId)) return@filterIndexed false
                    // ...and (CR 608.2b) the player-target restriction still holds. A player who
                    // gained life above the threshold, or whose "lost life this turn" never
                    // happened, is removed at resolution.
                    val requirement = getRequirementForTargetIndex(index, targetRequirements)
                    val restriction = when (requirement) {
                        is TargetPlayer -> requirement.restriction
                        is TargetOpponent -> requirement.restriction
                        else -> null
                    }
                    PlayerTargetRestriction.isSatisfied(state, restriction, target.playerId, controllerId, sourceId)
                }

                is ChosenTarget.Permanent -> {
                    // Permanent is valid if still on battlefield
                    if (target.entityId !in state.getBattlefield()) return@filterIndexed false

                    // Check shroud — can't be targeted by anyone (Rule 702.18)
                    if (projected.hasKeyword(target.entityId, "SHROUD")) return@filterIndexed false

                    // Check hexproof — can't be targeted by opponents (Rule 702.11)
                    val entityController = projected.getController(target.entityId)
                        ?: state.getEntity(target.entityId)?.get<ControllerComponent>()?.playerId
                    val hexproofSuppressed = HexproofSuppression.isSuppressedForCaster(state, projected, target.entityId, controllerId)
                    if (!hexproofSuppressed && projected.hasKeyword(target.entityId, "HEXPROOF") && entityController != controllerId) return@filterIndexed false

                    // Check hexproof from color (Rule 702.11b)
                    if (!hexproofSuppressed && entityController != controllerId) {
                        for (color in sourceColors) {
                            if (projected.hasKeyword(target.entityId, "HEXPROOF_FROM_${color.name}")) {
                                return@filterIndexed false
                            }
                        }
                    }

                    // Check can't-be-targeted-by-abilities (Shanna, Sisay's Legacy)
                    if (targetingSourceType != TargetingSourceType.SPELL && entityController != controllerId) {
                        val container = state.getEntity(target.entityId)
                        if (container?.has<CantBeTargetedByOpponentAbilitiesComponent>() == true) {
                            return@filterIndexed false
                        }
                    }

                    // Check protection from source colors/subtypes (Rule 702.16)
                    for (color in sourceColors) {
                        if (projected.hasKeyword(target.entityId, "PROTECTION_FROM_${color.name}")) {
                            return@filterIndexed false
                        }
                    }
                    for (subtype in sourceSubtypes) {
                        if (projected.hasKeyword(target.entityId, "PROTECTION_FROM_SUBTYPE_${subtype.uppercase()}")) {
                            return@filterIndexed false
                        }
                    }

                    // Check protection from each opponent (Rule 702.16e)
                    if (projected.hasKeyword(target.entityId, "PROTECTION_FROM_EACH_OPPONENT") &&
                        entityController != null && entityController != controllerId) {
                        return@filterIndexed false
                    }

                    // Re-validate target filter (Rule 608.2b)
                    val requirement = getRequirementForTargetIndex(index, targetRequirements)
                    val filter = extractTargetFilter(requirement)
                    if (filter != null) {
                        if (!predicateEvaluator.matches(
                                state, projected, target.entityId, filter.baseFilter, predicateContext
                            )
                        ) {
                            return@filterIndexed false
                        }
                    }

                    true
                }

                is ChosenTarget.Card -> {
                    // Card is valid if in expected zone
                    val zoneKey = ZoneKey(target.ownerId, target.zone)
                    target.cardId in state.getZone(zoneKey)
                }

                is ChosenTarget.Spell -> {
                    // Spell is valid if still on stack
                    target.spellEntityId in state.stack
                }
            }
        }
    }

    /**
     * Project [validTargets] (the compacted output of [validateTargets]) back onto
     * [originalTargets] positions, returning a list parallel to [originalTargets] with
     * `null` in slots whose target was dropped by 608.2b validation. Walks both lists
     * in order — [validateTargets] preserves the relative ordering of survivors — so the
     * mapping is unambiguous even when two original targets compare structurally equal.
     */
    private fun buildAlignedValidated(
        originalTargets: List<ChosenTarget>,
        validTargets: List<ChosenTarget>
    ): List<ChosenTarget?> {
        var v = 0
        return originalTargets.map { orig ->
            if (v < validTargets.size && validTargets[v] === orig) {
                v++
                orig
            } else {
                null
            }
        }
    }

    /**
     * Find the TargetRequirement that corresponds to a given target index.
     * Requirements are matched to targets in order, with each requirement
     * consuming `count` targets.
     */
    private fun getRequirementForTargetIndex(
        targetIndex: Int,
        requirements: List<TargetRequirement>
    ): TargetRequirement? {
        var idx = 0
        for (req in requirements) {
            val end = idx + req.count
            if (targetIndex in idx until end) return req
            idx = end
        }
        return null
    }

    /**
     * Extract the TargetFilter from a TargetRequirement, if it has one.
     */
    private fun extractTargetFilter(requirement: TargetRequirement?): TargetFilter? {
        return when (requirement) {
            is TargetObject -> requirement.filter
            else -> null
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Determine which zone a card is being cast from. Called internally by [castSpell] (before the
     * card is removed from its origin zone) and by `CastSpellHandler` to stamp `castFromZone` on the
     * turn's [com.wingedsheep.engine.state.CastSpellRecord]; both invoke it while the card is still
     * in its origin zone so they agree on the result.
     */
    internal fun findCastFromZone(
        state: GameState,
        cardId: EntityId,
        playerId: EntityId
    ): Zone? {
        val zones = listOf(Zone.HAND, Zone.GRAVEYARD, Zone.LIBRARY, Zone.COMMAND)
        for (zone in zones) {
            if (cardId in state.getZone(ZoneKey(playerId, zone))) {
                return zone
            }
        }
        // Check all players' exile zones (cards may be in another player's exile,
        // e.g., Villainous Wealth exiles from opponent's library)
        for (pid in state.turnOrder) {
            if (cardId in state.getZone(ZoneKey(pid, Zone.EXILE))) {
                return Zone.EXILE
            }
        }
        return null
    }

    /**
     * Remove a card from its current zone (for casting).
     */
    private fun removeFromCurrentZone(
        state: GameState,
        cardId: EntityId,
        playerId: EntityId
    ): GameState {
        // Try removing from hand first
        val handZone = ZoneKey(playerId, Zone.HAND)
        if (cardId in state.getZone(handZone)) {
            return state.removeFromZone(handZone, cardId)
        }

        // Also check graveyard (for flashback etc.)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        if (cardId in state.getZone(graveyardZone)) {
            return state.removeFromZone(graveyardZone, cardId)
        }

        // Check all players' exile zones (cards may be in another player's exile,
        // e.g., Villainous Wealth exiles from opponent's library)
        for (pid in state.turnOrder) {
            val exileZone = ZoneKey(pid, Zone.EXILE)
            if (cardId in state.getZone(exileZone)) {
                // A suspended card cast out of exile is no longer suspended (CR 702.62) — drop
                // the marker so it doesn't ride along onto the resulting permanent (which reuses
                // this entity id). The exile-side countdown trigger is gated on time counters,
                // so a leftover marker would be inert, but this keeps the permanent clean.
                val removed = state.removeFromZone(exileZone, cardId)
                    .updateEntity(cardId) { it.without<com.wingedsheep.engine.state.components.battlefield.SuspendedComponent>() }
                return com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                    .unlinkFromAllLinkedExiles(removed, cardId)
            }
        }

        // Check library (for Future Sight / play from top of library)
        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        if (cardId in state.getZone(libraryZone)) {
            return state.removeFromZone(libraryZone, cardId)
        }

        // Check the command zone (Commander format casts).
        val commandZone = ZoneKey(playerId, Zone.COMMAND)
        if (cardId in state.getZone(commandZone)) {
            return state.removeFromZone(commandZone, cardId)
        }

        return state
    }

    /**
     * Once a player casts a face-down morph, opponents can no longer know whether
     * any previously revealed morph card is still in that player's hand.
     */
    private fun clearRevealedMorphsInHand(state: GameState, playerId: EntityId): GameState {
        var newState = state
        for (handCardId in state.getZone(ZoneKey(playerId, Zone.HAND))) {
            val container = newState.getEntity(handCardId) ?: continue
            if (!container.has<HasMorphAbilityComponent>()) continue
            if (container.get<RevealedToComponent>() == null) continue

            newState = newState.updateEntity(handCardId) { c ->
                c.without<RevealedToComponent>()
            }
        }
        return newState
    }

    /**
     * Check if a spell on the stack is granted "can't be countered" by any permanent
     * on the battlefield with a GrantCantBeCountered static ability.
     *
     * The predicate context's `controllerId` is set to the source permanent's controller
     * so filters using `youControl()` correctly mean "the granter's controller controls X"
     * (e.g., Hexing Squelcher's "Spells you control can't be countered" should only protect
     * its own controller's spells, not every player's spells).
     */
    private fun isGrantedCantBeCountered(state: GameState, spellId: EntityId): Boolean {
        for (playerId in state.turnOrder) {
            for (entityId in state.getBattlefield(playerId)) {
                val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
                val def = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                val sourceControllerId =
                    state.getEntity(entityId)?.get<ControllerComponent>()?.playerId ?: playerId
                val context = PredicateContext(controllerId = sourceControllerId, sourceId = entityId)
                for (ability in def.staticAbilities) {
                    if (ability is GrantCantBeCountered) {
                        if (predicateEvaluator.matches(state, state.projectedState, spellId, ability.filter, context)) {
                            return true
                        }
                    }
                }
            }
        }

        // Player-scoped grant: "Creature spells you cast this turn can't be countered" (Domri,
        // Anarch of Bolas). The granter is the spell's controller, so we evaluate filters from
        // their SpellsCantBeCounteredComponent against the spell on the stack.
        val spellController = state.getEntity(spellId)
            ?.get<SpellOnStackComponent>()
            ?.casterId
            ?: state.getEntity(spellId)?.get<ControllerComponent>()?.playerId
        if (spellController != null) {
            val component = state.getEntity(spellController)
                ?.get<com.wingedsheep.engine.state.components.player.SpellsCantBeCounteredComponent>()
            if (component != null) {
                val context = PredicateContext(controllerId = spellController, sourceId = spellController)
                for (filter in component.filters) {
                    if (predicateEvaluator.matches(state, state.projectedState, spellId, filter, context)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    // =========================================================================
    // Valiant / "first time targeted" tracking
    // =========================================================================

    /**
     * Check if the target entity has already been targeted by the given controller this turn.
     */
    private fun hasBeenTargetedByController(state: GameState, targetId: EntityId, controllerId: EntityId): Boolean {
        val component = state.getEntity(targetId)?.get<TargetedByControllerThisTurnComponent>()
        return component?.hasBeenTargetedBy(controllerId) == true
    }

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


    /**
     * Create the appropriate decision and continuation for an EntersWithChoice replacement effect.
     * Returns null if the choice cannot be presented (e.g., no creatures on battlefield for CREATURE_ON_BATTLEFIELD).
     */
    internal fun pauseForEntersWithChoice(
        state: GameState,
        spellId: EntityId,
        controllerId: EntityId,
        ownerId: EntityId,
        cardComponent: CardComponent,
        choice: EntersWithChoice
    ): ExecutionResult? {
        val chooserId = when (choice.chooser) {
            com.wingedsheep.sdk.scripting.references.Player.AnOpponent ->
                state.getOpponents(controllerId).firstOrNull() ?: controllerId
            else -> controllerId
        }

        return when (choice.choiceType) {
            ChoiceType.COLOR -> {
                val decisionId = "choose-color-enters-${spellId.value}"
                val decision = ChooseColorDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = "Choose a color",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    )
                )
                val continuation = EntersWithChoiceSpellContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    choiceType = ChoiceType.COLOR
                )
                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                ExecutionResult.paused(pausedState, decision)
            }

            ChoiceType.CREATURE_TYPE -> {
                val creatureTypeOptions = choice.allowedCreatureTypes
                    ?: com.wingedsheep.sdk.core.Subtype.ALL_CREATURE_TYPES
                val decisionId = "choose-creature-type-enters-${spellId.value}"
                val decision = ChooseOptionDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = "Choose a creature type",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = creatureTypeOptions,
                    defaultSearch = ""
                )
                val continuation = EntersWithChoiceSpellContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    choiceType = ChoiceType.CREATURE_TYPE,
                    creatureTypes = creatureTypeOptions
                )
                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                ExecutionResult.paused(pausedState, decision)
            }

            ChoiceType.CREATURE_ON_BATTLEFIELD -> {
                val battlefieldCreatures = state.getBattlefield().filter { entityId ->
                    val container = state.getEntity(entityId) ?: return@filter false
                    val card = container.get<CardComponent>() ?: return@filter false
                    val controller = container.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId ?: return@filter false
                    controller == controllerId && card.typeLine.isCreature && entityId != spellId
                }
                if (battlefieldCreatures.isEmpty()) return null // No creatures — enter without choice
                val decisionId = "choose-creature-enters-${spellId.value}"
                val decision = SelectCardsDecision(
                    id = decisionId,
                    playerId = controllerId,
                    prompt = "Choose another creature you control",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = battlefieldCreatures,
                    minSelections = 1,
                    maxSelections = 1,
                    useTargetingUI = true
                )
                val continuation = EntersWithChoiceSpellContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    choiceType = ChoiceType.CREATURE_ON_BATTLEFIELD
                )
                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                ExecutionResult.paused(pausedState, decision)
            }

            ChoiceType.MODE -> {
                if (choice.modeOptions.isEmpty()) {
                    return null
                }
                val decisionId = "choose-mode-enters-${spellId.value}"
                val decision = ChooseOptionDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = "Choose for ${cardComponent.name}",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = choice.modeOptions.map { it.label },
                    optionMetadata = choice.modeOptions.map {
                        OptionMetadata(id = it.id, description = it.description, iconKey = it.iconKey)
                    }
                )
                val continuation = EntersWithChoiceSpellContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    choiceType = ChoiceType.MODE,
                    modeOptionIds = choice.modeOptions.map { it.id }
                )
                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                ExecutionResult.paused(pausedState, decision)
            }

            ChoiceType.BASIC_LAND_TYPE -> {
                val landTypeOptions = com.wingedsheep.sdk.core.Subtype.ALL_BASIC_LAND_TYPES.toList()
                val decisionId = "choose-land-type-enters-${spellId.value}"
                val decision = ChooseOptionDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = "Choose a basic land type",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = landTypeOptions,
                    defaultSearch = ""
                )
                val continuation = EntersWithChoiceSpellContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    choiceType = ChoiceType.BASIC_LAND_TYPE,
                    landTypes = landTypeOptions
                )
                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                ExecutionResult.paused(pausedState, decision)
            }

            ChoiceType.OPPONENT -> {
                // CR 614.12a — replacement-effect choices that modify how a permanent enters
                // are made before the permanent enters. We surface the opponent prompt now so
                // the chosen opponent is durably recorded in [CastChoicesComponent]. In a 1v1
                // game this collapses to a forced choice but the prompt is still surfaced.
                val opponentIds = state.turnOrder.filter { it != chooserId }
                if (opponentIds.isEmpty()) return null
                val opponentNames = opponentIds.map { pid ->
                    state.getEntity(pid)
                        ?.get<com.wingedsheep.engine.state.components.identity.PlayerComponent>()?.name
                        ?: "Player ${pid.value}"
                }
                val decisionId = "choose-opponent-enters-${spellId.value}"
                val decision = ChooseOptionDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = "Choose an opponent",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = opponentNames
                )
                val continuation = EntersWithChoiceSpellContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    choiceType = ChoiceType.OPPONENT,
                    opponentIds = opponentIds
                )
                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                ExecutionResult.paused(pausedState, decision)
            }
        }
    }

}

/**
 * Build pipeline `storedCollections` for cost-chosen card IDs.
 *
 * The chosen IDs (from [AdditionalCost.Behold], [AdditionalCost.BeholdOrPay], or
 * [AdditionalCost.ChooseEntity]) are stored on the stack object as
 * [SpellOnStackComponent.beheldCards]. Each of those costs declares its own
 * `storeAs` key that the card's resolution-time effects reference (e.g. via
 * `EntityReference.FromCostStorage`). To keep the effect's reference
 * stable across cost variants, expose the IDs under every relevant `storeAs`
 * key plus a default `"beheld"` key for backward compatibility with
 * pre-existing Behold-using cards.
 *
 * Top-level so non-stack consumers (e.g. the client-side preview text builder
 * in `ClientStateTransformer`) can populate the same pipeline view of the
 * spell's cost-chosen state without re-implementing the lookup.
 */
internal fun buildBeheldStoredCollections(
    beheldCards: List<EntityId>,
    cardDef: com.wingedsheep.sdk.model.CardDefinition?
): Map<String, List<EntityId>> {
    if (beheldCards.isEmpty()) return emptyMap()
    val keys = mutableSetOf("beheld")
    cardDef?.script?.additionalCosts?.forEach { cost ->
        val flat = if (cost is AdditionalCost.Composite) cost.steps else listOf(cost)
        for (c in flat) {
            when (c) {
                is AdditionalCost.Behold -> keys += c.storeAs
                is AdditionalCost.BeholdOrPay -> keys += c.storeAs
                is AdditionalCost.ChooseEntity -> keys += c.storeAs
                else -> {}
            }
        }
    }
    return keys.associateWith { beheldCards }
}
