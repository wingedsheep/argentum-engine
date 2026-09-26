package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.mechanics.FlashbackGrants
import com.wingedsheep.engine.mechanics.HarmonizeGrants
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.AfterResolveDestinationComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TextChanges
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.*

/**
 * Resolves a spell (CR 608.2): re-checks its targets (CR 608.2b), fizzles it when every target has
 * become illegal, and otherwise routes it to [PermanentSpellResolver] or [NonPermanentSpellResolver]
 * by the type line of the face it was cast as.
 */
internal class SpellResolver(
    private val cardRegistry: CardRegistry,
    private val predicateEvaluator: PredicateEvaluator,
    private val targetValidator: ResolutionTargetValidator,
    private val permanentSpellResolver: PermanentSpellResolver,
    private val nonPermanentSpellResolver: NonPermanentSpellResolver
) {
    /**
     * Resolve a spell.
     */
    fun resolveSpell(
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
            // 608.2b re-checks targets against the spell's text as it is now (CR 613.1c) — the
            // stack keeps the printed requirements, so a text change that began or ended while
            // the spell waited is honoured.
            val spellText = TextChanges.of(state, spellId)
            val validTargets = targetValidator.validateTargets(
                state, targetsComponent.targets, sourceColors, sourceSubtypes,
                spellComponent.casterId,
                targetsComponent.targetRequirements.map { req -> spellText?.let { req.applyTextReplacement(it) } ?: req },
                sourceId = spellId,
                targetingSourceType = TargetingSourceType.SPELL,
                xValue = spellComponent.xValue,
                targetEntryStamps = targetsComponent.targetEntryStamps
            )
            if (validTargets.isEmpty()) {
                // All targets invalid - spell fizzles
                return fizzleSpell(state, spellId, cardComponent, spellComponent)
            }
            resolvedTargets = validTargets
            alignedResolvedTargets = targetValidator.buildAlignedValidated(targetsComponent.targets, validTargets)
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
            val permanentResult = permanentSpellResolver.resolvePermanentSpell(newState, spellId, spellComponent, cardComponent)
            if (permanentResult.outcome is Outcome.Paused) {
                return ExecutionResult.propagatePause(
                    permanentResult.state,
                    events + permanentResult.events
                )
            }
            newState = permanentResult.state
            events.addAll(permanentResult.events)
            // CR 708.2a — a permanent that entered face down has no name, so neither the
            // "resolved" line nor the "entered the battlefield" line may carry the printed one.
            // The cast line has its own event-time client presentation; leaving these two
            // audience-agnostic log events unmasked made the log contradict it and told the
            // opponent exactly what they were looking at.
            // Read the resolved entity rather than `spellComponent.castFaceDown` so every route
            // that lands a permanent face down is covered, not only a face-down cast.
            val permanentName = nameVisibleToAll(newState, spellId, cardComponent?.name ?: "Unknown")
            events.add(ResolvedEvent(spellId, permanentName))

        } else {
            // Execute effects and put in graveyard
            val effectResult = nonPermanentSpellResolver.resolveNonPermanentSpell(
                newState, spellId, spellComponent, cardComponent,
                resolvedTargets,
                alignedResolvedTargets
            )
            if (effectResult.outcome is Outcome.Paused) {
                // The spell remains on the stack until its final continuation completes.
                val allEvents = events + effectResult.events
                return ExecutionResult.propagatePause(
                    effectResult.state,
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
            (FlashbackGrants.effectiveFlashback(
                state, spellId, cardDef, spellComponent.casterId, cardRegistry, predicateEvaluator
            ) != null ||
                HarmonizeGrants.effectiveHarmonize(state, spellId, cardDef) != null)
        val exileAfterResolveComp = state.getEntity(spellId)?.get<AfterResolveDestinationComponent>()
        // Goliath Daydreamer-style components only redirect on actual resolution; if the spell
        // fizzles or is countered they go to graveyard normally.
        val riderOnFizzle = exileAfterResolveComp?.takeIf { !it.onlyIfResolved }
        // A fizzled spell heading to its owner's graveyard is a card put into a graveyard
        // "from anywhere" — honor RedirectZoneChange replacements (Valgavoth, Leyline).
        val fizzleRedirect = if (flashbackExile || riderOnFizzle != null) {
            com.wingedsheep.engine.handlers.effects.ZoneChangeRedirectResult(
                riderOnFizzle?.zone ?: Zone.EXILE
            )
        } else {
            com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .checkZoneChangeRedirect(state, spellId, Zone.STACK, Zone.GRAVEYARD, predicateEvaluator = predicateEvaluator)
        }
        val destZone = fizzleRedirect.destinationZone
        val destZoneKey = ZoneKey(ownerId, destZone)

        var newState = state.updateEntity(spellId) { c ->
            c.without<SpellOnStackComponent>().without<TargetsComponent>()
        }
        newState = newState.addToZone(destZoneKey, spellId)
        val destinationObject = newState.objectRef(spellId)
        // A card-intrinsic redirect into the library shuffles the card in (Progenitus).
        if (destZone == Zone.LIBRARY && fizzleRedirect.shuffleIntoLibrary) {
            newState = SpellZoneMoves.shuffleOwnerLibrary(newState, ownerId)
        }
        if (destZone == Zone.EXILE && fizzleRedirect.linkSourceId != null) {
            newState = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .linkExiledToSource(newState, spellId, fizzleRedirect.linkSourceId)
        }

        return ExecutionResult.success(
            newState,
            listOf(
                SpellFizzledEvent(spellId, cardComponent?.name ?: "Unknown", "All targets are invalid"),
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
}
