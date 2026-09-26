package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.AfterResolveDestinationComponent
import com.wingedsheep.engine.state.components.identity.CantBeCounteredComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.ExileCounteredSpellInstead
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GrantCantBeCountered
import com.wingedsheep.sdk.scripting.effects.LibraryChoicePosition
import com.wingedsheep.sdk.scripting.targets.*

/**
 * Removes a spell or ability from the stack without resolving it: countering (CR 701.6) — to the
 * graveyard, to hand, or to exile — and exiling a spell outright, which is not a counter.
 *
 * One instance per engine ([com.wingedsheep.engine.core.EngineServices.spellCounterer]), shared by
 * the [StackResolver] façade and the counter / exile-a-spell executors. It needs nothing from the
 * resolution machinery, which is what lets an executor counter a spell without reaching for a
 * whole [StackResolver] — and the effect handler that comes with it.
 */
class SpellCounterer(
    private val cardRegistry: CardRegistry,
    private val predicateEvaluator: PredicateEvaluator
) {
    /**
     * Counter whatever stack object [entityId] is, spell or ability.
     *
     * A countered spell goes to its owner's graveyard; a countered ability simply ceases to
     * exist. "Counter it unless you pay …" effects — ward above all — can end up pointed at
     * either kind, so they route through here instead of assuming a spell: [counterSpell] on a
     * triggered ability finds no card/spell component and errors out, leaving the ability on the
     * stack to resolve as though the cost had been paid.
     */
    fun counterSpellOrAbility(
        state: GameState,
        entityId: EntityId,
        countererId: EntityId? = null
    ): ExecutionResult {
        val container = state.getEntity(entityId)
            ?: return ExecutionResult.error(state, "Stack object not found: $entityId")
        return if (container.has<SpellOnStackComponent>()) counterSpell(state, entityId, countererId)
        else counterAbility(state, entityId)
    }

    /**
     * Counter a spell on the stack.
     *
     * @param countererId the controller of the spell or ability doing the countering — the "you"
     *   of a counter replacement ([ExileCounteredSpellInstead], Guile). Null when unknown, in which
     *   case no such replacement applies.
     */
    fun counterSpell(state: GameState, spellId: EntityId, countererId: EntityId? = null): ExecutionResult {
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

        exileInsteadOfCounter(state, spellId, countererId)?.let { return it }

        val spellComponent = container.get<SpellOnStackComponent>()
        val ownerId = cardComponent?.ownerId
            ?: spellComponent?.casterId
            ?: return ExecutionResult.error(state, "Cannot determine spell owner")

        // Remove from stack
        var newState = state.removeFromStack(spellId)

        // Put in graveyard (or exile if AfterResolveDestinationComponent is present)
        // Goliath Daydreamer-style components only exile on actual resolution; if the spell
        // is countered they go to graveyard normally.
        val riderOnCounter = container.get<AfterResolveDestinationComponent>()
            ?.takeIf { !it.onlyIfResolved }
        // A countered spell heading to its owner's graveyard is still a card being put into a
        // graveyard "from anywhere" — honor RedirectZoneChange replacements (Valgavoth, Leyline).
        val counterRedirect = if (riderOnCounter != null) {
            com.wingedsheep.engine.handlers.effects.ZoneChangeRedirectResult(riderOnCounter.zone)
        } else {
            com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .checkZoneChangeRedirect(state, spellId, Zone.STACK, Zone.GRAVEYARD, predicateEvaluator = predicateEvaluator)
        }
        val destZone = counterRedirect.destinationZone
        val destZoneKey = ZoneKey(ownerId, destZone)
        newState = newState.addToZone(destZoneKey, spellId)
        val destinationObject = newState.objectRef(spellId)
        // A card-intrinsic redirect into the library shuffles the card in (Progenitus).
        if (destZone == Zone.LIBRARY && counterRedirect.shuffleIntoLibrary) {
            newState = SpellZoneMoves.shuffleOwnerLibrary(newState, ownerId)
        }
        if (destZone == Zone.EXILE && counterRedirect.linkSourceId != null) {
            newState = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .linkExiledToSource(newState, spellId, counterRedirect.linkSourceId)
        }

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
                    Zone.STACK,
                    destZone,
                    ownerId, oldObject = state.objectRef(spellId), newObject = destinationObject
                )
            )
        )
    }

    /**
     * Counter a spell on the stack and put it into its owner's **hand** instead of their
     * graveyard (Remand). If the spell can't be countered, nothing happens — the spell resolves
     * and is *not* returned, which is Remand's 2021-03-19 ruling.
     *
     * The sibling of [counterSpellToExile] on the other destination, and it keeps the two things
     * that make this a counter rather than a bounce: the [SpellCounteredEvent] still fires, and
     * an [AfterResolveDestinationComponent] that applies on a counter (a flashback card's exile
     * replacement) still wins over the hand, exactly as it does over the graveyard in
     * [counterSpell].
     */
    fun counterSpellToHand(state: GameState, spellId: EntityId, countererId: EntityId? = null): ExecutionResult =
        counterSpellIntoOwnersZone(state, spellId, countererId, Zone.HAND, position = null)

    /**
     * Counter a spell on the stack and put it into its owner's **library** at [position] instead
     * of their graveyard (Memory Lapse's top, Hinder's chosen end). The library sibling of
     * [counterSpellToHand], with the same counter semantics: an uncounterable spell is untouched,
     * a counter replacement (Guile) or a flashback rider still wins, and a
     * [SpellCounteredEvent] fires.
     *
     * Everyone saw the spell and where it went, so the card is marked revealed to every player at
     * its new slot — the same courtesy the non-counter "put target spell on top or bottom" move
     * gives.
     */
    fun counterSpellToLibrary(
        state: GameState,
        spellId: EntityId,
        position: LibraryChoicePosition,
        countererId: EntityId? = null
    ): ExecutionResult = counterSpellIntoOwnersZone(state, spellId, countererId, Zone.LIBRARY, position)

    /**
     * Whether countering [spellId] now would actually send it to the countering effect's printed
     * destination — it can be countered, and neither a counter replacement
     * ([ExileCounteredSpellInstead]) nor the spell's own on-counter rider (flashback's exile)
     * would send it elsewhere. A destination that needs a choice (Hinder's top or bottom) asks only
     * when this holds, so an uncounterable spell never prompts for a pointless choice.
     */
    fun wouldReachCounterDestination(state: GameState, spellId: EntityId, countererId: EntityId?): Boolean {
        val container = state.getEntity(spellId) ?: return false
        if (spellId !in state.stack) return false
        if (container.has<CantBeCounteredComponent>() || isGrantedCantBeCountered(state, spellId)) return false
        if (container.get<AfterResolveDestinationComponent>()?.takeIf { !it.onlyIfResolved } != null) return false
        return findExileInsteadReplacement(state, countererId) == null
    }

    /**
     * The shared body of [counterSpellToHand] and [counterSpellToLibrary]: a counter whose printed
     * destination is one of the owner's non-graveyard zones.
     *
     * Neither a hand nor a library is reachable by `RedirectZoneChange` replacements the way a
     * graveyard is (those key on "put into a graveyard from anywhere"), so no redirect check runs
     * here; an on-counter [AfterResolveDestinationComponent] (flashback's exile) is the only thing
     * that can move the destination.
     */
    private fun counterSpellIntoOwnersZone(
        state: GameState,
        spellId: EntityId,
        countererId: EntityId?,
        printedZone: Zone,
        position: LibraryChoicePosition?
    ): ExecutionResult {
        if (spellId !in state.stack) {
            return ExecutionResult.error(state, "Spell not on stack: $spellId")
        }

        val container = state.getEntity(spellId)
            ?: return ExecutionResult.error(state, "Spell not found: $spellId")

        val cardComponent = container.get<CardComponent>()

        if (container.has<CantBeCounteredComponent>() || isGrantedCantBeCountered(state, spellId)) {
            return ExecutionResult.success(state)
        }

        exileInsteadOfCounter(state, spellId, countererId)?.let { return it }

        val spellComponent = container.get<SpellOnStackComponent>()
        val ownerId = cardComponent?.ownerId
            ?: spellComponent?.casterId
            ?: return ExecutionResult.error(state, "Cannot determine spell owner")

        var newState = state.removeFromStack(spellId)

        // A flashback/foretell-style "exile it instead" rider that applies on a counter still
        // overrides the printed destination — the same precedence [counterSpell] gives it.
        val riderOnCounter = container.get<AfterResolveDestinationComponent>()
            ?.takeIf { !it.onlyIfResolved }
        val destZone = riderOnCounter?.zone ?: printedZone
        val destZoneKey = ZoneKey(ownerId, destZone)
        newState = if (riderOnCounter == null && destZone == Zone.LIBRARY && position != null) {
            val librarySize = newState.getZone(destZoneKey).size
            val index = when (position) {
                LibraryChoicePosition.Top -> 0
                LibraryChoicePosition.SecondFromTop -> minOf(1, librarySize)
                LibraryChoicePosition.Bottom -> librarySize
            }
            newState.insertIntoZone(destZoneKey, spellId, index)
        } else {
            newState.addToZone(destZoneKey, spellId)
        }
        val destinationObject = newState.objectRef(spellId)

        newState = newState.updateEntity(spellId) { c ->
            c.without<SpellOnStackComponent>().without<TargetsComponent>()
        }
        if (destZone == Zone.LIBRARY) {
            newState = LibraryRevealUtils
                .markRevealed(newState, listOf(spellId), newState.turnOrder.toSet())
        }

        return ExecutionResult.success(
            newState,
            listOf(
                SpellCounteredEvent(spellId, cardComponent?.name ?: "Unknown"),
                ZoneChangeEvent(
                    spellId,
                    cardComponent?.name ?: "Unknown",
                    Zone.STACK,
                    destZone,
                    ownerId, oldObject = state.objectRef(spellId), newObject = destinationObject
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

        exileInsteadOfCounter(state, spellId, controllerId)?.let { return it }

        val spellComponent = container.get<SpellOnStackComponent>()
        val ownerId = cardComponent?.ownerId
            ?: spellComponent?.casterId
            ?: return ExecutionResult.error(state, "Cannot determine spell owner")

        // Remove from stack
        var newState = state.removeFromStack(spellId)

        // Put in exile (instead of graveyard)
        val exileZone = ZoneKey(ownerId, Zone.EXILE)
        newState = newState.addToZone(exileZone, spellId)

        // Remove stack components and optionally grant the counter's controller a free recast
        // (Kheru Spellsnatcher).
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
                    Zone.STACK,
                    Zone.EXILE,
                    ownerId, oldObject = state.objectRef(spellId), newObject = newState.objectRef(spellId)
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
     *
     * When [fixedAlternativeManaCost] is non-null the exiled card's **owner** gets a permanent
     * may-play permission and a [PlayWithFixedAlternativeManaCostComponent], letting them recast it
     * for that fixed cost instead of its printed cost for as long as it stays exiled — the
     * spell-on-stack form of the **Airbend** keyword (Aang, Swift Savior). Mutually exclusive with
     * [makePlotted].
     */
    fun exileSpell(
        state: GameState,
        spellId: EntityId,
        makePlotted: Boolean,
        fixedAlternativeManaCost: com.wingedsheep.sdk.core.ManaCost? = null,
        linkToSourceId: EntityId? = null
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
            ZoneChangeEvent(spellId, cardComponent?.name ?: "Unknown", Zone.STACK, Zone.EXILE, ownerId,
                oldObject = state.objectRef(spellId), newObject = newState.objectRef(spellId))
        )

        if (makePlotted) {
            newState = SpellZoneMoves.applyPlottedToExiledCard(newState, spellId, ownerId, cardComponent?.name ?: "Unknown", events)
        } else if (fixedAlternativeManaCost != null) {
            newState = applyFixedAltCostToExiledCard(newState, spellId, ownerId, fixedAlternativeManaCost)
        }

        // "Exile it with this permanent" (Spell Queller): record the card in the source's
        // linked-exile pile so a later ability of that source can say "the exiled card". Only a
        // handle — nothing returns or becomes castable on its own.
        if (linkToSourceId != null) {
            newState = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .linkExiledToSource(newState, spellId, linkToSourceId)
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Grant the **owner** of a card already sitting in their exile a permanent may-play permission
     * plus a [PlayWithFixedAlternativeManaCostComponent], so they may recast it for [fixedCost]
     * instead of its printed cost for as long as it stays exiled. The spell-on-stack tail of the
     * **Airbend** keyword; mirrors [applyPlottedToExiledCard] but with a fixed alternative cost
     * rather than a free, plotted-on-a-later-turn cast.
     */
    private fun applyFixedAltCostToExiledCard(
        state: GameState,
        cardId: EntityId,
        ownerId: EntityId,
        fixedCost: com.wingedsheep.sdk.core.ManaCost,
    ): GameState {
        var newState = state.updateEntity(cardId) { c ->
            c.with(
                com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent(
                    controllerId = ownerId,
                    fixedCost = fixedCost
                )
            )
        }
        val (permId, stateWithPerm) = newState.newEntity()
        newState = stateWithPerm.addMayPlayPermission(
            MayPlayPermission(
                id = permId,
                cardIds = setOf(cardId),
                controllerId = ownerId,
                permanent = true,
                timestamp = newState.timestamp,
            )
        )
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

        val triggeredAbility = container.get<TriggeredAbilityOnStackComponent>()
        val activatedAbility = container.get<ActivatedAbilityOnStackComponent>()
        val description = triggeredAbility?.description
            ?: activatedAbility?.let { "${it.sourceName}'s ability" }
            ?: "Unknown ability"
        // Read the source and controller off the stack object while it still exists — the ability
        // is about to cease to exist, and this is the last point either is knowable.
        val sourceId = triggeredAbility?.sourceId ?: activatedAbility?.sourceId
        val sourceName = triggeredAbility?.sourceName ?: activatedAbility?.sourceName
        val controllerId = triggeredAbility?.controllerId ?: activatedAbility?.controllerId

        // "Abilities can't be countered" (Spider-Punk): a battlefield GrantCantBeCountered with
        // includesAbilities = true whose filter matches this ability makes the counter fizzle — the
        // ability stays on the stack and resolves normally.
        if (isAbilityGrantedCantBeCountered(state, abilityId)) {
            return ExecutionResult.success(state)
        }

        // A countered ability is cancelled and removed from the stack (Rule 701.6a); it goes to no
        // zone, and like a resolved ability (Rule 608.2n) it ceases to exist. Destroying the entity
        // as well as the stack entry keeps that symmetry with the resolution paths, which all call
        // removeEntity — otherwise every countered ability lingers in `entities` for the rest of the
        // game and rides along in every serialized GameState.
        val newState = state.removeEntity(abilityId)

        return ExecutionResult.success(
            newState,
            listOf(
                AbilityCounteredEvent(
                    abilityEntityId = abilityId,
                    description = description,
                    sourceId = sourceId,
                    sourceName = sourceName,
                    controllerId = controllerId,
                )
            )
        )
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
    /**
     * "If a spell or ability you control would counter a spell, instead exile that spell and you
     * may play that card without paying its mana cost" — [ExileCounteredSpellInstead] (Guile).
     *
     * Called by every counter routine once the spell is known to be counterable, so a spell that
     * can't be countered is never exiled. When a battlefield permanent's replacement matches
     * [countererId], the spell goes to its owner's exile *instead* of being countered — no
     * [SpellCounteredEvent], and no counter destination rider (Remand's hand, flashback's exile)
     * applies, since nothing was countered. The rest of the replacement is queued as a
     * [com.wingedsheep.engine.replacement.PendingReplacementRider] and runs as soon as the
     * countering instruction finishes. A copy of a spell ceases to exist once it's exiled
     * (CR 707.10a), so it has no rest to run.
     *
     * Returns null when no such replacement applies and the caller counters the spell as usual.
     */
    private fun exileInsteadOfCounter(
        state: GameState,
        spellId: EntityId,
        countererId: EntityId?
    ): ExecutionResult? {
        val (host, hostController, replacement) = findExileInsteadReplacement(state, countererId) ?: return null

        val container = state.getEntity(spellId) ?: return null
        val cardComponent = container.get<CardComponent>()
        val ownerId = cardComponent?.ownerId
            ?: container.get<SpellOnStackComponent>()?.casterId
            ?: return null
        val isSpellCopy = container.get<CopyOfComponent>()?.originalCardComponent == null &&
            container.has<CopyOfComponent>()

        var newState = state.removeFromStack(spellId)
        newState = newState.addToZone(ZoneKey(ownerId, Zone.EXILE), spellId)
        newState = newState.updateEntity(spellId) { c ->
            c.without<SpellOnStackComponent>().without<TargetsComponent>()
        }
        val then = replacement.then
        if (then != null && !isSpellCopy) {
            newState = newState.copy(
                pendingReplacementRiders = newState.pendingReplacementRiders +
                    com.wingedsheep.engine.replacement.PendingReplacementRider(
                        effect = then,
                        hostId = host,
                        controllerId = hostController,
                        subjectId = spellId
                    )
            )
        }
        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    spellId,
                    cardComponent?.name ?: "Unknown",
                    Zone.STACK,
                    Zone.EXILE,
                    ownerId, oldObject = state.objectRef(spellId), newObject = newState.objectRef(spellId)
                )
            )
        )
    }

    /** The first battlefield [ExileCounteredSpellInstead] that applies to [countererId]'s counter: host, its controller, the replacement. */
    private fun findExileInsteadReplacement(
        state: GameState,
        countererId: EntityId?
    ): Triple<EntityId, EntityId, ExileCounteredSpellInstead>? {
        if (countererId == null) return null
        val projected = state.projectedState
        for (entityId in state.getBattlefield()) {
            val effects = state.getEntity(entityId)?.get<ReplacementEffectSourceComponent>()?.replacementEffects
                ?: continue
            val controller = projected.getController(entityId)
                ?: state.getEntity(entityId)?.get<ControllerComponent>()?.playerId
                ?: continue
            for (effect in effects) {
                if (effect !is ExileCounteredSpellInstead) continue
                val pattern = effect.appliesTo as? EventPattern.CounterSpellEvent ?: continue
                val countererMatches = when (pattern.counterer) {
                    Player.You -> countererId == controller
                    Player.EachOpponent -> countererId != controller
                    else -> true
                }
                if (countererMatches) return Triple(entityId, controller, effect)
            }
        }
        return null
    }

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

    /**
     * Whether an activated/triggered ability on the stack ([abilityId]) can't be countered because a
     * battlefield [GrantCantBeCountered] with `includesAbilities = true` covers it (its filter matches
     * the ability — an unrestricted `GameObjectFilter.Any` matches every ability). Spider-Punk's
     * "Spells and abilities can't be countered."
     */
    private fun isAbilityGrantedCantBeCountered(state: GameState, abilityId: EntityId): Boolean {
        for (playerId in state.turnOrder) {
            for (entityId in state.getBattlefield(playerId)) {
                val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
                val def = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                val sourceControllerId =
                    state.getEntity(entityId)?.get<ControllerComponent>()?.playerId ?: playerId
                val context = PredicateContext(controllerId = sourceControllerId, sourceId = entityId)
                for (ability in def.staticAbilities) {
                    if (ability is GrantCantBeCountered && ability.includesAbilities) {
                        if (predicateEvaluator.matches(state, state.projectedState, abilityId, ability.filter, context)) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }
}
