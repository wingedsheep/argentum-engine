package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.PutOntoBattlefieldAttachedToChosenContinuation
import com.wingedsheep.engine.core.TargetRequirementInfo
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EntersWithCountersHelper
import com.wingedsheep.engine.handlers.effects.LibraryPlacement
import com.wingedsheep.engine.handlers.effects.ZoneEntryOptions
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
import com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.destroyPermanent
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for MoveToZoneEffect.
 * Unified zone-moving effect that consolidates destroy, exile, bounce,
 * shuffle-into-library, put-on-top, etc.
 *
 * Delegates all zone movement to [ZoneTransitionService] for consistent cleanup.
 */
class MoveToZoneEffectExecutor(
    private val cardRegistry: CardRegistry,
    private val targetFinder: TargetFinder = TargetFinder()
) : EffectExecutor<MoveToZoneEffect> {

    override val effectType: KClass<MoveToZoneEffect> = MoveToZoneEffect::class

    override fun execute(
        state: GameState,
        effect: MoveToZoneEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for move to zone")

        // byDestruction delegates to destroyPermanent (handles indestructible)
        if (effect.byDestruction) {
            return destroyPermanent(state, targetId)
        }

        val container = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target entity not found: $targetId")

        val cardComponent = container.get<CardComponent>()
            ?: return EffectResult.error(state, "Target is not a card: $targetId")

        val ownerId = container.get<OwnerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return EffectResult.error(state, "Cannot determine card owner")

        val currentZone = findEntityZone(state, targetId)
            ?: return EffectResult.error(state, "Card not found in any zone: $targetId")

        // If fromZone is specified, skip the move if the target is not in the expected zone
        if (effect.fromZone != null && currentZone.zoneType != effect.fromZone) {
            return EffectResult.success(state, emptyList())
        }

        // Resolve controller override for "under your control" effects
        val controllerOverride = effect.controllerOverride
        val controllerId = if (controllerOverride != null && effect.destination == Zone.BATTLEFIELD) {
            context.resolveTarget(controllerOverride, state) ?: ownerId
        } else {
            ownerId
        }

        // CR 303.4g — an Aura entering the battlefield by any means other than resolving as an
        // Aura spell (here: reanimation / return from graveyard, exile, etc.) has its controller
        // choose what it enchants as it enters. Without that choice the Aura would enter
        // unattached and immediately die to a state-based action (CR 704.5n). Cast Auras attach
        // during stack resolution and never reach this executor, and the explicit
        // "attached to ..." effect has its own executor, so a generic move-to-battlefield of an
        // Aura is always the choose-as-it-enters case.
        if (effect.destination == Zone.BATTLEFIELD && effect.faceDown == null && cardComponent.typeLine.isAura) {
            return attachAuraOnEnter(state, targetId, cardComponent, controllerId, context)
        }

        // Build ZoneEntryOptions based on placement and effect properties
        val entryOptions = buildEntryOptions(effect, cardComponent, controllerId)

        val transitionResult = ZoneTransitionService.moveToZone(
            state, targetId, effect.destination, entryOptions, currentZone
        )

        var resultState = transitionResult.state
        val extraEvents = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        // Apply "enters with counters" replacement effects when a permanent enters the
        // battlefield from a non-stack zone (e.g., reanimation from graveyard, return from
        // exile). Spell resolution applies these in StackResolver, so this path covers
        // everything else. Skip face-down entries: morph creatures enter as 2/2 nameless
        // creatures with no replacement effects from their face-up identity.
        val actualDestZone = transitionResult.actualDestination
        if (actualDestZone == Zone.BATTLEFIELD && effect.faceDown == null) {
            val (counterState, counterEvents) = EntersWithCountersHelper.applyEntersWithCounters(
                resultState, targetId, controllerId, cardRegistry
            )
            resultState = counterState
            extraEvents.addAll(counterEvents)
        }

        // Link exiled card to source permanent via LinkedExileComponent
        if (effect.linkToSource && effect.destination == Zone.EXILE) {
            val sourceId = context.sourceId
                ?: return EffectResult.success(resultState, transitionResult.events + extraEvents)
            val sourceContainer = resultState.getEntity(sourceId)
                ?: return EffectResult.success(resultState, transitionResult.events + extraEvents)
            val existingLinked = sourceContainer.get<LinkedExileComponent>()
            val allExiled = (existingLinked?.exiledIds ?: emptyList()) + listOf(targetId)
            resultState = resultState.updateEntity(sourceId) { c ->
                c.with(LinkedExileComponent(allExiled))
            }
        }

        // Auto-reveal: when a card moves from a publicly visible zone (graveyard/exile)
        // back into a hidden zone (hand/library) or onto the battlefield, emit a
        // CardsRevealedEvent so the UI can surface what card moved and why. The card was
        // already public info, so this just surfaces the move in the reveal overlay.
        //
        // The "revealer" is the controller performing the move, not the card's owner: a
        // reanimation can put an opponent's graveyard card onto *your* battlefield under
        // *your* control (Shark Shredder), and the overlay must read as you doing it, not
        // as the opponent. controllerId only diverges from ownerId for a controller-override
        // move onto the battlefield (see above); returns to hand/library never carry an
        // override, so controllerId == ownerId there and behaviour is unchanged.
        val revealEvents = autoRevealForReturn(
            fromZone = currentZone.zoneType,
            toZone = effect.destination,
            targetId = targetId,
            cardName = cardComponent.name,
            imageUri = cardComponent.imageUri,
            controllerId = controllerId,
            sourceId = context.sourceId,
            state = resultState
        )

        return EffectResult.success(resultState, transitionResult.events + extraEvents + revealEvents)
    }

    /**
     * Handle an Aura that is being put onto the battlefield by something other than resolving as
     * an Aura spell (CR 303.4g). The controller chooses what it enchants from among everything it
     * can legally enchant (CR 303.4f — targeting restrictions like hexproof/shroud are ignored for
     * this choice). The actual move-and-attach happens in
     * [PutOntoBattlefieldAttachedToChosenContinuation], reused from the explicit "attached to"
     * effect so both paths wire `AttachedToComponent`, the host's `AttachmentsComponent`, and the
     * Aura's continuous/replacement effects identically.
     */
    private fun attachAuraOnEnter(
        state: GameState,
        cardId: EntityId,
        cardComponent: CardComponent,
        controllerId: EntityId,
        context: EffectContext
    ): EffectResult {
        val auraTarget = cardRegistry.getCard(cardComponent.cardDefinitionId)?.script?.auraTarget
        val legalHosts = if (auraTarget == null) emptyList() else targetFinder.findLegalTargets(
            state = state,
            requirement = auraTarget,
            controllerId = controllerId,
            sourceId = cardId,
            ignoreTargetingRestrictions = true
        )

        // No legal host — the Aura can't enter and stays in its current zone (CR 303.4g).
        if (legalHosts.isEmpty()) {
            return EffectResult.success(state)
        }

        val cardName = cardComponent.name
        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseTargetsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose what $cardName attaches to",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
                phase = DecisionPhase.RESOLUTION
            ),
            targetRequirements = listOf(
                TargetRequirementInfo(
                    index = 0,
                    description = "what $cardName attaches to",
                    minTargets = 1,
                    maxTargets = 1
                )
            ),
            legalTargets = mapOf(0 to legalHosts)
        )
        val continuation = PutOntoBattlefieldAttachedToChosenContinuation(
            decisionId = decisionId,
            cardId = cardId,
            controllerId = controllerId
        )
        val newState = state.withPendingDecision(decision).pushContinuation(continuation)
        return EffectResult(state = newState, events = emptyList(), pendingDecision = decision)
    }

    private fun autoRevealForReturn(
        fromZone: Zone,
        toZone: Zone,
        targetId: com.wingedsheep.sdk.model.EntityId,
        cardName: String,
        imageUri: String?,
        controllerId: com.wingedsheep.sdk.model.EntityId,
        sourceId: com.wingedsheep.sdk.model.EntityId?,
        state: GameState
    ): List<com.wingedsheep.engine.core.GameEvent> {
        val isFromPublic = fromZone == Zone.GRAVEYARD || fromZone == Zone.EXILE
        val isToReturnDestination =
            toZone == Zone.HAND || toZone == Zone.LIBRARY || toZone == Zone.BATTLEFIELD
        if (!isFromPublic || !isToReturnDestination) return emptyList()

        val sourceName = sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        return listOf(
            CardsRevealedEvent(
                revealingPlayerId = controllerId,
                cardIds = listOf(targetId),
                cardNames = listOf(cardName),
                imageUris = listOf(imageUri),
                source = sourceName,
                // The controller performed the move themselves — they already know what/where
                // it is. Only the other player needs the reveal overlay to surface the
                // transition (e.g. their card being reanimated onto their opponent's board).
                revealToSelf = false,
                fromZone = fromZone,
                toZone = toZone
            )
        )
    }

    /**
     * Build [ZoneEntryOptions] from the effect's placement and properties.
     */
    private fun buildEntryOptions(
        effect: MoveToZoneEffect,
        cardComponent: CardComponent,
        controllerId: com.wingedsheep.sdk.model.EntityId
    ): ZoneEntryOptions {
        val libraryPlacement = when {
            effect.positionFromTop != null && effect.destination == Zone.LIBRARY ->
                LibraryPlacement.NthFromTop(effect.positionFromTop!!)
            effect.placement == ZonePlacement.Top -> LibraryPlacement.Top
            effect.placement == ZonePlacement.Bottom -> LibraryPlacement.Bottom
            effect.placement == ZonePlacement.Shuffled -> LibraryPlacement.Shuffled
            else -> LibraryPlacement.Top
        }

        // Derive turn-up data for a face-down battlefield entry (morph cost vs. manifest mana cost).
        val faceDownMode = effect.faceDown
        val isBattlefieldFaceDown = faceDownMode != null && effect.destination == Zone.BATTLEFIELD
        val morphData = if (isBattlefieldFaceDown) {
            com.wingedsheep.engine.handlers.effects.FaceDownTurnUp.dataFor(
                cardRegistry.getCard(cardComponent.cardDefinitionId),
                cardComponent.cardDefinitionId,
                faceDownMode!!
            )
        } else null

        return ZoneEntryOptions(
            controllerId = controllerId,
            libraryPlacement = libraryPlacement,
            tapped = effect.placement == ZonePlacement.Tapped,
            tappedAndAttacking = effect.placement == ZonePlacement.TappedAndAttacking,
            faceDown = isBattlefieldFaceDown,
            morphData = morphData,
            manifested = isBattlefieldFaceDown && faceDownMode == FaceDownMode.MANIFEST
        )
    }

    private fun findEntityZone(state: GameState, entityId: com.wingedsheep.sdk.model.EntityId): com.wingedsheep.engine.state.ZoneKey? {
        for ((zoneKey, entities) in state.zones) {
            if (entityId in entities) {
                return zoneKey
            }
        }
        return null
    }
}
