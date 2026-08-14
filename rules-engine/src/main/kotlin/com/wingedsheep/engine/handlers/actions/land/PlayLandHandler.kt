package com.wingedsheep.engine.handlers.actions.land

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.permissions.activeMayPlayFor
import com.wingedsheep.engine.state.permissions.hasMayPlayFor
import com.wingedsheep.engine.state.permissions.removeMayPlayPermissionsForCard
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.PlayerCantPlayFromHandComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.battlefield.GraveyardPlayPermissionUsedComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.OnEnterRunEffect
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.sdk.scripting.MayPlayPermanentsFromGraveyard
import com.wingedsheep.engine.legalactions.utils.LandDropUtils
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary
import kotlin.reflect.KClass

/**
 * Handler for the PlayLand action.
 *
 * Playing a land is a special action that doesn't use the stack.
 * It moves the land from hand to battlefield and uses up a land drop.
 */
class PlayLandHandler(
    private val cardRegistry: CardRegistry,
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor,
    private val conditionEvaluator: ConditionEvaluator,
    private val effectExecutor: (com.wingedsheep.engine.state.GameState,
                                 com.wingedsheep.sdk.scripting.effects.Effect,
                                 com.wingedsheep.engine.handlers.EffectContext) ->
        com.wingedsheep.engine.core.EffectResult,
) : ActionHandler<PlayLand> {
    override val actionType: KClass<PlayLand> = PlayLand::class

    private val predicateEvaluator = com.wingedsheep.engine.handlers.PredicateEvaluator()

    override fun validate(state: GameState, action: PlayLand): String? {
        if (!state.isActiveTurnFor(action.playerId)) {
            // CR 805.4c — each player on the active team may play a land on the team's turn.
            return "You can only play lands on your turn"
        }
        if (!state.step.isMainPhase) {
            return "You can only play lands during a main phase"
        }
        if (state.stack.isNotEmpty()) {
            return "You can only play lands when the stack is empty"
        }

        // Check land drop availability (accounts for static ability bonuses)
        val landDrops = state.getEntity(action.playerId)?.get<LandDropsComponent>()
            ?: LandDropsComponent()
        val staticBonus =
            LandDropUtils.getAdditionalLandDrops(state, action.playerId, cardRegistry, conditionEvaluator)
        if (landDrops.remaining + staticBonus <= 0) {
            return "You have already played a land this turn"
        }

        // Check card exists and is a land
        val container = state.getEntity(action.cardId)
            ?: return "Card not found: ${action.cardId}"

        val cardComponent = container.get<CardComponent>()
            ?: return "Not a card: ${action.cardId}"

        if (!cardComponent.typeLine.isLand) {
            return "You can only play land cards as lands"
        }

        // Check card is in hand, on top of library with PlayFromTopOfLibrary, in exile with MayPlayPermission,
        // or in graveyard with MayPlayPermanentsFromGraveyard permission (Muldrotha)
        val handZone = ZoneKey(action.playerId, Zone.HAND)
        val inHand = action.cardId in state.getZone(handZone)
        val onTopOfLibrary = !inHand && isOnTopOfLibraryWithPermission(state, action.playerId, action.cardId)
        val mayPlayFromExile = !inHand && !onTopOfLibrary && isInExileWithPlayPermission(state, action.playerId, action.cardId)
        // Lands exiled with a permanent granting "you may play cards exiled with this" (Valgavoth).
        val mayPlayFromLinkedExile = !inHand && !onTopOfLibrary && !mayPlayFromExile &&
            com.wingedsheep.engine.handlers.effects.linkedexile.LinkedExilePlayUtils
                .canPlayLand(state, action.playerId, action.cardId, cardRegistry)
        val mayPlayFromGraveyard = !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayPlayFromLinkedExile &&
            isInGraveyardWithPlayPermission(state, action.playerId, action.cardId)
        if (!inHand && !onTopOfLibrary && !mayPlayFromExile && !mayPlayFromLinkedExile && !mayPlayFromGraveyard) {
            return "Land is not in your hand"
        }

        // Memory Vessel: "they can't play cards from their hand" — hand-scoped, so a land granted
        // a may-play permission from exile/graveyard still resolves.
        if (inHand && state.getEntity(action.playerId)?.has<PlayerCantPlayFromHandComponent>() == true) {
            return "You can't play cards from your hand"
        }

        return null
    }

    override fun execute(state: GameState, action: PlayLand): ExecutionResult {
        val container = state.getEntity(action.cardId)
            ?: return ExecutionResult.error(state, "Card not found")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")

        var newState = state

        // Remove from hand, library, exile, or graveyard (whichever zone the card is in)
        val handZone = ZoneKey(action.playerId, Zone.HAND)
        val libraryZone = ZoneKey(action.playerId, Zone.LIBRARY)
        val graveyardZone = ZoneKey(action.playerId, Zone.GRAVEYARD)
        // Check all exile zones since cards may be in another player's exile (Villainous Wealth)
        val exileOwner = state.turnOrder.firstOrNull { pid ->
            action.cardId in state.getZone(ZoneKey(pid, Zone.EXILE))
        }
        val fromZone = when {
            action.cardId in state.getZone(handZone) -> Zone.HAND
            action.cardId in state.getZone(libraryZone) -> Zone.LIBRARY
            exileOwner != null -> Zone.EXILE
            action.cardId in state.getZone(graveyardZone) -> Zone.GRAVEYARD
            else -> Zone.HAND
        }
        val sourceZoneKey = if (fromZone == Zone.EXILE && exileOwner != null) {
            ZoneKey(exileOwner, Zone.EXILE)
        } else {
            ZoneKey(action.playerId, fromZone)
        }
        newState = newState.removeFromZone(sourceZoneKey, action.cardId)

        // A land played from a face-down exile enters face up as the real land, never as a face-down
        // 2/2. Black Cat, Cunning Thief exiles cards face down (HIDDEN) and lets you play them; lands
        // bypass ZoneTransitionService/StackResolver — where every other play path reveals a
        // hidden-in-exile card (StackResolver strips FaceDownComponent as a spell hits the stack) —
        // so strip the marker here (CR 305.1: a land is always played face up). No-op when absent.
        newState = newState.updateEntity(action.cardId) { c -> c.without<FaceDownComponent>() }

        // Record Muldrotha graveyard land permission usage
        if (fromZone == Zone.GRAVEYARD) {
            newState = recordGraveyardPlayPermissionUsage(newState, action.playerId, CardType.LAND.name)
            // Mark "entered from a graveyard" for ETB conditions (Oscorp Industries: "when this land
            // enters from a graveyard, you lose 2 life"). Lands bypass ZoneTransitionService, which
            // normally stamps this component, so set it here.
            newState = newState.updateEntity(action.cardId) { c ->
                c.with(com.wingedsheep.engine.state.components.battlefield.EnteredFromGraveyardComponent)
            }
        }

        // The land-play signal (CR 305.1). [landPlayedEvent] rides alongside every entry
        // ZoneChangeEvent below (like [riderPlayEvent]) so "whenever you play a land …" triggers
        // (Shadow of the Goblin) fire — distinct from an effect *putting* a land onto the
        // battlefield, which emits only the ZoneChangeEvent. Playing from a non-hand zone also sets
        // the turn flag read by Spider-Man 2099's end-step condition.
        val landPlayedEvent = com.wingedsheep.engine.core.LandPlayedEvent(
            action.cardId, action.playerId, fromZone
        )
        // Record this land play's zone-of-origin for "played a land this turn from [anywhere other
        // than] zone X" conditions (Spider-Man 2099). Appended for every play — HAND included — so a
        // future card can gate on any specific origin, mirroring the spell path's castFromZone.
        newState = newState.updateEntity(action.playerId) { c ->
            val prior = c.get<com.wingedsheep.engine.state.components.player
                .LandsPlayedThisTurnComponent>()
                ?: com.wingedsheep.engine.state.components.player.LandsPlayedThisTurnComponent()
            c.with(prior.copy(fromZones = prior.fromZones + fromZone))
        }

        // Add controller component first so projection sees the right controller when
        // BattlefieldEntry.place records the ETB-by-type for this player.
        newState = newState.updateEntity(action.cardId) { c ->
            c.with(ControllerComponent(action.playerId))
        }
        newState = com.wingedsheep.engine.handlers.effects.BattlefieldEntry
            .place(newState, action.playerId, action.cardId)

        // Lands bypass ZoneTransitionService, which is where every other zone-change path
        // stamps EnteredThisTurnComponent (cleared again at the controller's next untap step,
        // see BeginningPhaseManager). Without this, "activate only if this land entered the
        // battlefield this turn" (Hidden Lair) and any other Conditions.SourceEnteredThisTurn /
        // GameObjectFilter.enteredThisTurn() check keyed on a land would never see it as true,
        // even on the very turn it was played.
        newState = newState.updateEntity(action.cardId) { c -> c.with(EnteredThisTurnComponent) }

        // Lands bypass ZoneTransitionService (which bakes ETB components for everything else),
        // so install the land's own static + replacement effect components here — mirroring
        // ZoneTransitionService's ETB baking and putPermanentOnBattlefield. Done once, before every
        // tapped/OnEnter/EntersWithChoice branch below, since the land is on the battlefield with its
        // controller set by now. Without this a land's printed statics never project once it's in
        // play: e.g. Secret Tunnel's "this land can't be blocked" (relevant once the land is
        // animated), man-land grants, Urborg's type-setting, Blood Moon-style effects.
        newState = newState.updateEntity(action.cardId) { c ->
            val staticAbilityHandler =
                com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler(cardRegistry)
            var updated = staticAbilityHandler.addContinuousEffectComponent(c)
            updated = staticAbilityHandler.addReplacementEffectComponent(updated)
            updated
        }

        // "Lands you control enter untapped" (The Wandering Minstrel) and similar EntersUntapped
        // replacement effects from other battlefield permanents override every tapped-entry path
        // below (permission-forced, shock-land "pay or tapped", conditional tapped duals). The
        // land is on the battlefield with its controller set, so the filter resolves correctly.
        val landEntersUntapped = com.wingedsheep.engine.handlers.effects.EnterUntappedReplacements
            .entersUntapped(newState, action.cardId, action.playerId)

        // A may-play permission with landEntersTapped=true forces the played land
        // tapped regardless of the card's own ETB script — Lightstall Inquisitor's
        // "each land played this way enters tapped" clause.
        val permissionForcesTapped = fromZone == Zone.EXILE && permissionForcesLandTapped(
            state, action.playerId, action.cardId
        )
        if (permissionForcesTapped && !landEntersUntapped) {
            newState = newState.updateEntity(action.cardId) { c -> c.with(TappedComponent) }
        }
        // "When you play a card this way, …" rider (Fires of Mount Doom). If this land was
        // played from exile via a may-play permission carrying a rider, capture the linked
        // event BEFORE removing the permission so the rider's delayed triggered ability fires.
        val riderPlayEvent: com.wingedsheep.engine.core.CardPlayedFromPermissionEvent? =
            if (fromZone == Zone.EXILE) {
                state.mayPlayPermissions.firstOrNull { permission ->
                    permission.riderLinkId != null &&
                        permission.controllerId == action.playerId &&
                        action.cardId in permission.cardIds &&
                        permission.sourceId != null
                }?.let { permission ->
                    com.wingedsheep.engine.core.CardPlayedFromPermissionEvent(
                        cardId = action.cardId,
                        controllerId = action.playerId,
                        sourceId = permission.sourceId!!,
                        linkId = permission.riderLinkId!!
                    )
                }
            } else null

        // Clean up may-play permissions now that the card has left exile. Lands don't go
        // through the stack, so StackResolver's removeMayPlayPermissionsForCard never runs
        // for them; without this, a permanent permission would silently re-authorize the
        // card if it later returned to exile.
        if (fromZone == Zone.EXILE) {
            newState = newState.removeMayPlayPermissionsForCard(action.cardId)
            // Drop the card from any linked-exile granter (Valgavoth) now that it has left exile,
            // so the granter no longer lists it. (Lands bypass ZoneTransitionService, which would
            // otherwise unlink on the way out.)
            newState = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .unlinkFromAllLinkedExiles(newState, action.cardId)
        }
        // A generic per-card may-play permission (Tablet of Discovery's "you may play that card
        // this turn") can also leave the card in the graveyard. Lands bypass the stack, so clean
        // up the permission here too — otherwise it would silently re-authorize the card if it
        // returned to the graveyard later this turn. No-op when no per-card permission exists.
        if (fromZone == Zone.GRAVEYARD) {
            newState = newState.removeMayPlayPermissionsForCard(action.cardId)
        }

        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)

        // OnEnterRunEffect — generic "as ~ enters, run [effect]" replacement.
        // Runs BEFORE the EntersTapped check so effects like
        // Effects.Tap(EffectTarget.Self) (Game Trail's "otherwise" rider) apply
        // synchronously with entry. May pause for player input via continuations.
        if (cardDef != null) {
            val onEnter = cardDef.script.replacementEffects
                .filterIsInstance<OnEnterRunEffect>()
                .firstOrNull()
            if (onEnter != null) {
                // Use up the land drop and emit the entry event before running the
                // effect — by this point the land is fully on the battlefield and
                // EffectTarget.Self resolves to it.
                newState = newState.updateEntity(action.playerId) { c ->
                    val landDrops = c.get<LandDropsComponent>() ?: LandDropsComponent()
                    c.with(landDrops.use())
                }
                val zoneChangeEvent = com.wingedsheep.engine.core.ZoneChangeEvent(
                    action.cardId,
                    cardComponent.name,
                    fromZone,
                    Zone.BATTLEFIELD,
                    action.playerId,
                )
                val onEnterEvents = mutableListOf<com.wingedsheep.engine.core.GameEvent>(zoneChangeEvent)
                riderPlayEvent?.let { onEnterEvents.add(it) }
                onEnterEvents.add(landPlayedEvent)
                newState = newState.tick()

                val effectContext = EffectContext(
                    sourceId = action.cardId,
                    controllerId = action.playerId,
                )
                val effectResult = effectExecutor(newState, onEnter.effect, effectContext)
                if (effectResult.isPaused) {
                    return ExecutionResult.paused(
                        effectResult.state,
                        effectResult.pendingDecision!!,
                        onEnterEvents + effectResult.events,
                    )
                }
                newState = effectResult.state
                onEnterEvents.addAll(effectResult.events)

                // Fire any triggers from the land entering (landfall, etc.).
                val triggers = triggerDetector.detectTriggers(newState, onEnterEvents)
                if (triggers.isNotEmpty()) {
                    val triggerResult = triggerProcessor.processTriggers(newState, triggers)
                    val allEvents = onEnterEvents + triggerResult.events
                    if (triggerResult.isPaused) {
                        return ExecutionResult.paused(triggerResult.state, triggerResult.pendingDecision!!, allEvents)
                    }
                    return ExecutionResult.success(triggerResult.newState, allEvents)
                }
                return ExecutionResult.success(newState, onEnterEvents)
            }
        }

        // EntersAsCopy — "you may have this land enter [tapped] as a copy of a land card in a
        // graveyard, except it's a Cave …" (Echoing Deeps; also Vesuva / Thespian's Stage copying a
        // land on the battlefield). This is an as-enters replacement (CR 707.2): pause for the copy
        // choice; [CloneEntersOnBattlefieldContinuation]'s resumer copies the chosen land's copiable
        // characteristics onto this land, adds the extra subtype, taps it if the rider says so, and
        // fires the entry's ETB triggers. Only engaged when a candidate exists — otherwise the land
        // just enters as its printed self via the normal finish below.
        if (cardDef != null) {
            val entersAsCopy = cardDef.script.replacementEffects
                .filterIsInstance<EntersAsCopy>()
                .firstOrNull()
            if (entersAsCopy != null &&
                com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
                    .entersAsCopyCandidates(newState, action.cardId, action.playerId, entersAsCopy)
                    .isNotEmpty()
            ) {
                // Use up a land drop before pausing. The entry ZoneChangeEvent is emitted by the
                // resumer once the copy choice is made, so it carries the final (copied) identity
                // and `copyOfOriginalName` — see CloneEntersOnBattlefieldContinuation's resumer.
                // Emitting a printed-name event here too would double the entry and hide the copy.
                // Only the play-from-permission rider event (if any) is surfaced now.
                newState = newState.updateEntity(action.playerId) { c ->
                    val landDrops = c.get<LandDropsComponent>() ?: LandDropsComponent()
                    c.with(landDrops.use())
                }
                newState = newState.tick()

                val result = com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
                    .pauseForEntersAsCopy(
                        state = newState,
                        entityId = action.cardId,
                        controllerId = action.playerId,
                        cardComponent = cardComponent,
                        effect = entersAsCopy,
                        fromZone = fromZone,
                        carryEvents = listOfNotNull(riderPlayEvent, landPlayedEvent),
                    )
                if (result != null) return result
            }
        }

        // Check for "enters the battlefield tapped" replacement effect. Skipped entirely when an
        // EntersUntapped effect (The Wandering Minstrel) applies — the land falls through to the
        // normal untapped finish, so a shock land's "pay life or enter tapped" prompt is elided
        // (paying is moot when the land enters untapped regardless).
        if (cardDef != null && !landEntersUntapped) {
            val entersTapped = cardDef.script.replacementEffects.filterIsInstance<EntersTapped>().firstOrNull()
            if (entersTapped != null) {
                // Permission already forced tapped — skip the shock-land "pay life" prompt
                // and any conditional script logic; the land is already on the battlefield tapped.
                //
                // Strict CR 616.1 says both replacements (Lightstall's "enters tapped" rider
                // and the shock land's "you may pay 2 life; otherwise enters tapped") apply to
                // the entry event and the affected permanent's controller picks which to resolve
                // first. Neither is a self-replacement effect (CR 614.15), so the choice falls to
                // the catch-all step CR 616.1e. Either order ends with the land tapped, so the
                // shock-land life-payment choice is always
                // meaningless under a permission-forced-tapped grant. We elide the prompt
                // rather than asking the player to make a no-op decision.
                if (permissionForcesTapped) {
                    // Already handled: TappedComponent applied, control falls through to triggers/finish.
                } else if (entersTapped.payLifeCost != null) {
                    // Shock land: ask the player if they want to pay life
                    // Use up a land drop first
                    newState = newState.updateEntity(action.playerId) { c ->
                        val landDrops = c.get<LandDropsComponent>() ?: LandDropsComponent()
                        c.with(landDrops.use())
                    }

                    val zoneChangeEvent = com.wingedsheep.engine.core.ZoneChangeEvent(
                        action.cardId,
                        cardComponent.name,
                        fromZone,
                        Zone.BATTLEFIELD,
                        action.playerId
                    )
                    val events = listOf(zoneChangeEvent) + listOfNotNull(riderPlayEvent, landPlayedEvent)
                    newState = newState.tick()

                    val decisionId = "pay-life-or-enter-tapped-${action.cardId.value}"
                    val decision = com.wingedsheep.engine.core.YesNoDecision(
                        id = decisionId,
                        playerId = action.playerId,
                        prompt = "Pay ${entersTapped.payLifeCost} life to have ${cardComponent.name} enter untapped?",
                        context = com.wingedsheep.engine.core.DecisionContext(
                            sourceId = action.cardId,
                            sourceName = cardComponent.name,
                            phase = com.wingedsheep.engine.core.DecisionPhase.RESOLUTION
                        )
                    )
                    val continuation = com.wingedsheep.engine.core.PayLifeOrEnterTappedLandContinuation(
                        decisionId = decisionId,
                        landId = action.cardId,
                        controllerId = action.playerId,
                        lifeCost = entersTapped.payLifeCost!!,
                        fromZone = fromZone
                    )
                    val pausedState = newState
                        .pushContinuation(continuation)
                        .withPendingDecision(decision)
                    return ExecutionResult.paused(pausedState, decision, events)
                } else {
                    val shouldEnterTapped = if (entersTapped.unlessCondition != null) {
                        // Conditional: enters tapped UNLESS condition is met
                        val context = EffectContext(
                            sourceId = action.cardId,
                            controllerId = action.playerId,
                        )
                        !ConditionEvaluator().evaluate(newState, entersTapped.unlessCondition!!, context)
                    } else {
                        true
                    }
                    if (shouldEnterTapped) {
                        newState = newState.updateEntity(action.cardId) { c ->
                            c.with(TappedComponent)
                        }
                    }
                }
            }
        }

        // Global "[filter] enter tapped" from another battlefield permanent (Zhao, the Moon
        // Slayer's "Nonbasic lands enter tapped"). Consulted after the land's own EntersTapped
        // replacement and gated on !landEntersUntapped so an EntersUntapped effect wins (CR 614).
        // Idempotent with a permission-forced / self tapland tap above (re-adding TappedComponent
        // is a no-op).
        if (!landEntersUntapped &&
            com.wingedsheep.engine.handlers.effects.EnterTappedReplacements
                .entersTapped(newState, action.cardId, action.playerId)
        ) {
            newState = newState.updateEntity(action.cardId) { c -> c.with(TappedComponent) }
        }

        // Check for "as enters, choose X" replacement effect (color or creature type)
        // Process first choice in priority order: COLOR → CREATURE_TYPE
        // Continuations handle chaining to subsequent choices.
        if (cardDef != null) {
            val printedChoices = cardDef.script.replacementEffects.filterIsInstance<EntersWithChoice>()
            // Granted Riot (a land that is a Spider with riot granted — vanishingly rare, but wired
            // for consistency with the token/spell entry seams; one choice per grant, CR 702.136b).
            val grantedRiotCount = com.wingedsheep.engine.mechanics.RiotSynthesis
                .grantedRiotInstanceCount(newState, action.cardId, cardRegistry, predicateEvaluator)
            val syntheticRiotChoice = if (grantedRiotCount > 0) {
                com.wingedsheep.engine.mechanics.RiotSynthesis.RIOT_CHOICE
            } else null
            val firstChoice = (printedChoices + listOfNotNull(syntheticRiotChoice))
                .sortedBy { it.choiceType.ordinal }
                .firstOrNull()
            if (firstChoice != null) {
                // Use up a land drop first
                newState = newState.updateEntity(action.playerId) { c ->
                    val landDrops = c.get<LandDropsComponent>() ?: LandDropsComponent()
                    c.with(landDrops.use())
                }

                val zoneChangeEvent = ZoneChangeEvent(
                    action.cardId,
                    cardComponent.name,
                    fromZone,
                    Zone.BATTLEFIELD,
                    action.playerId
                )
                val events = listOf(zoneChangeEvent) + listOfNotNull(riderPlayEvent, landPlayedEvent)
                newState = newState.tick()

                // Build the choice prompt + entity-keyed continuation via the shared on-battlefield
                // entry helper (also used by Momir token minting). The land is already on the
                // battlefield; the resumer records the choice and fires entry triggers afterward.
                val result = com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
                    .pauseForEntersWithChoice(
                        state = newState,
                        entityId = action.cardId,
                        controllerId = action.playerId,
                        cardComponent = cardComponent,
                        choice = firstChoice,
                        fromZone = fromZone,
                        carryEvents = events,
                        cardNameOptions = if (firstChoice.choiceType == ChoiceType.CARD_NAME) {
                            cardRegistry.cardNamesIn(firstChoice.cardNamePool).toList()
                        } else emptyList(),
                        syntheticRiot = firstChoice === syntheticRiotChoice,
                        syntheticRiotRemaining = if (firstChoice === syntheticRiotChoice) grantedRiotCount - 1 else 0,
                    )
                if (result != null) return result
            }
        }

        // Use up a land drop
        newState = newState.updateEntity(action.playerId) { c ->
            val landDrops = c.get<LandDropsComponent>() ?: LandDropsComponent()
            c.with(landDrops.use())
        }

        val zoneChangeEvent = ZoneChangeEvent(
            action.cardId,
            cardComponent.name,
            fromZone,
            Zone.BATTLEFIELD,
            action.playerId
        )

        val events = listOf(zoneChangeEvent) + listOfNotNull(riderPlayEvent, landPlayedEvent)
        newState = newState.tick()

        // Detect and process any triggers from the land entering (e.g., landfall)
        val triggers = triggerDetector.detectTriggers(newState, events)
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(newState, triggers)

            if (triggerResult.isPaused) {
                return ExecutionResult.paused(
                    triggerResult.state,
                    triggerResult.pendingDecision!!,
                    events + triggerResult.events
                )
            }

            return ExecutionResult.success(
                triggerResult.newState,
                events + triggerResult.events
            )
        }

        return ExecutionResult.success(newState, events)
    }

    private fun isOnTopOfLibraryWithPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val library = state.getLibrary(playerId)
        if (library.isEmpty() || library.first() != cardId) return false
        return hasPlayFromTopOfLibrary(state, playerId)
    }

    private fun isInExileWithPlayPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val inAnyExile = state.turnOrder.any { pid ->
            cardId in state.getZone(ZoneKey(pid, Zone.EXILE))
        }
        if (!inAnyExile) return false
        // A nonLandOnly permission ("you may cast that card" wording, e.g. Ragavan, Nimble
        // Pilferer) never authorizes playing a land — mirrors CastFromZoneEnumerator's gate.
        return state.activeMayPlayFor(cardId, playerId, conditionEvaluator, cardRegistry)
            .any { !it.nonLandOnly }
    }

    /**
     * True when some active may-play permission authorizing [playerId] to play [cardId]
     * also forces the played land tapped (Lightstall Inquisitor's "each land played this
     * way enters tapped" clause). Read from [state] *before* the card left exile, since
     * `removeMayPlayPermissionsForCard` runs as the card moves to the battlefield.
     *
     * A `nonLandOnly` permission never authorizes a land play in the first place (mirrors
     * [isInExileWithPlayPermission]'s filter) — its `landEntersTapped` rider is therefore only
     * relevant when it's actually the permission the land played through, never as a rider
     * leaking onto a land authorized by a *different*, plain permission.
     */
    private fun permissionForcesLandTapped(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean = state.activeMayPlayFor(cardId, playerId, conditionEvaluator, cardRegistry)
        .any { !it.nonLandOnly && it.landEntersTapped }

    private fun hasPlayFromTopOfLibrary(state: GameState, playerId: EntityId): Boolean {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.script.staticAbilities) {
                // Honor a conditional gate (e.g. The Lunar Whale's "as long as it attacked this
                // turn") against the granting permanent before allowing the land play from top.
                val unwrapped = if (ability is ConditionalStaticAbility) {
                    if (!evaluateStaticGate(state, ability.condition, entityId, playerId)) continue
                    ability.ability
                } else ability
                if (unwrapped is PlayFromTopOfLibrary || unwrapped is PlayLandsAndCastFilteredFromTopOfLibrary) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Check if a land card is in the player's graveyard and there's permission to play it.
     * Handles both Crucible-style (MayPlayLandsFromGraveyard, no usage tracking) and
     * Muldrotha-style (MayPlayPermanentsFromGraveyard, per-type usage tracked).
     */
    private fun isInGraveyardWithPlayPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        if (cardId !in state.getZone(graveyardZone)) return false
        // Generic per-card may-play permission (e.g. Tablet of Discovery's "mill a card. You
        // may play that card this turn." — the milled card sits in the graveyard). Mirrors the
        // exile path in [isInExileWithPlayPermission] and CastFromZoneEnumerator, which already
        // offers the PlayLand action for such a card; without this the handler would reject the
        // very action the enumerator advertised.
        // A nonLandOnly permission ("you may cast that card" wording) never authorizes playing
        // a land — mirrors isInExileWithPlayPermission.
        if (state.activeMayPlayFor(cardId, playerId, conditionEvaluator, cardRegistry)
                .any { !it.nonLandOnly }) return true
        if (hasLandGraveyardPlayPermission(state, playerId)) return true
        if (findGraveyardPlayPermissionSource(state, playerId, CardType.LAND.name) != null) return true
        // Mayhem (CR 702.187c): a Mayhem land discarded this turn may be played from the graveyard.
        val discardedThisTurn = state.getEntity(playerId)
            ?.get<com.wingedsheep.engine.state.components.player.CardsDiscardedThisTurnComponent>()
            ?.cardIds ?: emptyList()
        if (cardId in discardedThisTurn) {
            val cardDef = state.getEntity(cardId)?.get<CardComponent>()
                ?.let { cardRegistry.getCard(it.cardDefinitionId) }
            if (cardDef != null &&
                com.wingedsheep.engine.mechanics.MayhemGrants.effectiveMayhem(state, cardId, cardDef) != null
            ) return true
        }
        return false
    }

    /**
     * Returns true if the player controls a permanent with [MayPlayLandsFromGraveyard]
     * (Crucible of Worlds style — no per-turn usage tracking needed).
     */
    private fun hasLandGraveyardPlayPermission(state: GameState, playerId: EntityId): Boolean {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = state.getEntity(entityId)?.get<ClassLevelComponent>()?.currentLevel
            for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                // Unwrap mode/condition-gated abilities (e.g. Glacierwood Siege's Sultai mode)
                // and honor the gate against this source permanent.
                if (ability is ConditionalStaticAbility) {
                    if (ability.ability is MayPlayLandsFromGraveyard &&
                        evaluateStaticGate(state, ability.condition, entityId, playerId)
                    ) {
                        return true
                    }
                } else if (ability is MayPlayLandsFromGraveyard) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Evaluate a [ConditionalStaticAbility] gating condition against a specific source
     * permanent. Used so a mode/condition-gated graveyard-play permission only applies
     * while its gate holds (e.g. Glacierwood Siege only when "Sultai" is the chosen mode).
     */
    private fun evaluateStaticGate(
        state: GameState,
        condition: com.wingedsheep.sdk.scripting.conditions.Condition,
        sourceId: EntityId,
        controllerId: EntityId
    ): Boolean {
        val context = EffectContext(
            sourceId = sourceId,
            controllerId = controllerId,
        )
        return conditionEvaluator.evaluate(state, condition, context)
    }

    /**
     * Find a battlefield permanent controlled by the player that has MayPlayPermanentsFromGraveyard
     * and hasn't used its permission for the given type this turn.
     */
    private fun findGraveyardPlayPermissionSource(
        state: GameState,
        playerId: EntityId,
        typeName: String
    ): EntityId? {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is MayPlayPermanentsFromGraveyard }) {
                val tracker = state.getEntity(entityId)?.get<GraveyardPlayPermissionUsedComponent>()
                if (tracker == null || !tracker.hasUsedType(typeName)) {
                    return entityId
                }
            }
        }
        return null
    }

    /**
     * Record that a Muldrotha-like permanent's graveyard play permission was used for a type.
     */
    private fun recordGraveyardPlayPermissionUsage(
        state: GameState,
        playerId: EntityId,
        typeName: String
    ): GameState {
        val sourceId = findGraveyardPlayPermissionSource(state, playerId, typeName) ?: return state
        return state.updateEntity(sourceId) { c ->
            val tracker = c.get<GraveyardPlayPermissionUsedComponent>() ?: GraveyardPlayPermissionUsedComponent()
            c.with(tracker.withUsedType(typeName))
        }
    }

    companion object {
        fun create(services: EngineServices): PlayLandHandler {
            return PlayLandHandler(
                services.cardRegistry,
                services.triggerDetector,
                services.triggerProcessor,
                services.conditionEvaluator,
                services.effectExecutorRegistry::execute,
            )
        }
    }
}
