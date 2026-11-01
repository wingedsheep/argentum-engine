package com.wingedsheep.engine.handlers.actions.land

import com.wingedsheep.engine.state.components.battlefield.liveStaticAbilities
import com.wingedsheep.engine.legality.LegalityKernel
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.effects.permanent.types.setDfcFace
import com.wingedsheep.engine.handlers.effects.permanent.types.stampDoubleFacedFrontFace
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.permissions.activeMayPlayFor
import com.wingedsheep.engine.state.permissions.spendCappedMayPlayFor
import com.wingedsheep.engine.state.permissions.oncePerTurnGranterToSpend
import com.wingedsheep.engine.state.permissions.hasMayPlayFor
import com.wingedsheep.engine.state.permissions.matchesEntersTappedRiders
import com.wingedsheep.engine.state.permissions.mayPlayEntersTappedRiders
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
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard
import com.wingedsheep.engine.state.components.identity.emblemStaticAbilitiesOf
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.sdk.scripting.MayPlayPermanentsFromGraveyard
import com.wingedsheep.engine.legalactions.utils.LandDropUtils
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary
import com.wingedsheep.engine.handlers.actions.spell.allowsLand
import kotlin.reflect.KClass
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.faceUpCardDefinition

/**
 * Handler for the PlayLand action.
 *
 * Playing a land is a special action that doesn't use the stack.
 * It moves the land from hand to battlefield and uses up a land drop.
 */
class PlayLandHandler(
    private val cardRegistry: CardRegistry,
    private val conditionEvaluator: ConditionEvaluator,
    private val effectExecutor: (com.wingedsheep.engine.state.GameState,
                                 com.wingedsheep.sdk.scripting.effects.Effect,
                                 com.wingedsheep.engine.handlers.EffectContext) ->
        com.wingedsheep.engine.core.EffectResult,
    private val legality: LegalityKernel
) : ActionHandler<PlayLand> {
    private val predicateEvaluator = conditionEvaluator.predicates
    private val zoneResolver = com.wingedsheep.engine.handlers.actions.spell.CastZoneResolver(
        cardRegistry, conditionEvaluator, legality
    )
    override val actionType: KClass<PlayLand> = PlayLand::class

    override fun validate(state: GameState, action: PlayLand): String? =
        validate(state, action, duringResolution = false)

    private fun validate(state: GameState, action: PlayLand, duringResolution: Boolean): String? {
        if (!state.isActiveTurnFor(action.playerId)) {
            // CR 805.4c — each player on the active team may play a land on the team's turn.
            return "You can only play lands on your turn"
        }
        if (!duringResolution && !state.step.isMainPhase) {
            return "You can only play lands during a main phase"
        }
        if (!duringResolution && state.stack.isNotEmpty()) {
            return "You can only play lands when the stack is empty"
        }

        // Blanket "players can't play lands" lock (Worms of the Earth). Checked before the land
        // drop so the message says why, rather than blaming a drop the player still has.
        if (LandDropUtils.playerCantPlayLands(state, action.playerId, cardRegistry, conditionEvaluator)) {
            return "You can't play lands"
        }

        // Check land drop availability (accounts for static ability bonuses)
        if (!LandDropUtils.hasLandPlayLeft(state, action.playerId, cardRegistry, conditionEvaluator)) {
            return "You have already played a land this turn"
        }

        // Check card exists and is a land
        val container = state.getEntity(action.cardId)
            ?: return "Card not found: ${action.cardId}"

        val cardComponent = container.get<CardComponent>()
            ?: return "Not a card: ${action.cardId}"

        // CR 712.11c's play-a-land analogue: only the face being played is evaluated. A modal DFC
        // played as its back face is legal on the strength of *that* face's type line, and the
        // printed front's (which is what `cardComponent` carries off the battlefield, CR 712.8a)
        // is not consulted at all.
        val playedFace = if (action.asBackFace) {
            com.wingedsheep.engine.mechanics.ModalDfcCasts
                .landFace(cardRegistry.getCard(cardComponent.cardDefinitionId))
                ?: return "${cardComponent.name} has no land back face to play"
        } else null
        if (playedFace == null && !cardComponent.typeLine.isLand) {
            return "You can only play land cards as lands"
        }

        // Filtered "players can't play <these> lands" lock (City in a Bottle). The blanket probe
        // above deliberately ignores filtered locks, so the named card is asked about here — the
        // mirror of PlayLandEnumerator's per-candidate check.
        if (LandDropUtils.playerCantPlayLands(
                state, action.playerId, cardRegistry, conditionEvaluator, landCardId = action.cardId
            )
        ) {
            return "You can't play ${cardComponent.name}"
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
                .canPlayLand(state, action.playerId, action.cardId, legality)
        val mayPlayFromGraveyard = !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayPlayFromLinkedExile &&
            isInGraveyardWithPlayPermission(state, action.playerId, action.cardId)
        // A resolving effect that instructs "you may play that card" (Djinn of Wishes) can reach into
        // the library itself, not only its top under a static grant. Its executor grants a per-card
        // permission for the duration of the play; honoured only mid-resolution, so a stray library
        // permission can never surface as a land play at priority.
        val mayPlayFromLibraryByEffect = duringResolution && !inHand && !onTopOfLibrary &&
            isInLibraryWithPlayPermission(state, action.playerId, action.cardId)
        // "You may play a card you own from outside the game this turn" (Wish): a land in the
        // player's own sideboard under a stored may-play permission.
        val mayPlayFromSideboard = !inHand && isInSideboardWithPlayPermission(state, action.playerId, action.cardId)
        if (!inHand && !onTopOfLibrary && !mayPlayFromExile && !mayPlayFromLinkedExile && !mayPlayFromGraveyard &&
            !mayPlayFromLibraryByEffect && !mayPlayFromSideboard
        ) {
            return "Land is not in your hand"
        }

        // Memory Vessel: "they can't play cards from their hand" — hand-scoped, so a land granted
        // a may-play permission from exile/graveyard still resolves.
        if (inHand && state.getEntity(action.playerId)?.has<PlayerCantPlayFromHandComponent>() == true) {
            return "You can't play cards from your hand"
        }
        // "… and can't play it" on this very hand object (Firestorm Phoenix).
        if (com.wingedsheep.engine.mechanics.HandObjectRestrictions.isCantBePlayed(state, action.cardId)) {
            return "${cardComponent.name} can't be played right now"
        }

        return null
    }

    /**
     * Would [executeDuringResolution] accept [action] in [state]? Null when the land can be played
     * mid-resolution, otherwise why not (not your turn, no land play left, a lock, …).
     */
    fun validateDuringResolution(state: GameState, action: PlayLand): String? =
        validate(state, action, duringResolution = true)

    /** Play a land when a resolving effect explicitly instructs the player to do so. */
    fun executeDuringResolution(state: GameState, action: PlayLand): ExecutionResult {
        val validationError = validate(state, action, duringResolution = true)
        return if (validationError != null) ExecutionResult.error(state, validationError)
        else execute(state, action)
    }

    /**
     * Playing a land is a special action (CR 116.2a): it uses no stack and the player keeps
     * priority afterwards. The settle boundary puts the land's entry triggers (landfall) on the
     * stack and checks state-based actions before that player receives priority again (CR 704.3).
     * That check is what culls a second copy of a legendary land (CR 704.5j), and it runs after
     * detection, so a landfall trigger whose land the legend rule removes still fires (CR 603.10).
     */
    override fun execute(state: GameState, action: PlayLand): ExecutionResult {
        val played = executePlay(state, action)
        // A land creature that owes its controller an entry-copy order (CR 616.1): ask, then replay.
        return com.wingedsheep.engine.handlers.effects.copy.GrantedEntryCopy
            .askIfUnasked(state, played.state) { com.wingedsheep.engine.core.PlayLandReplay(action) } ?: played
    }

    private fun executePlay(state: GameState, action: PlayLand): ExecutionResult {
        val container = state.getEntity(action.cardId)
            ?: return ExecutionResult.error(state, "Card not found")

        val printedCardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")

        var newState = state

        // Remove from hand, library, exile, or graveyard (whichever zone the card is in)
        val handZone = ZoneKey(action.playerId, Zone.HAND)
        val libraryZone = ZoneKey(action.playerId, Zone.LIBRARY)
        val graveyardZone = ZoneKey(action.playerId, Zone.GRAVEYARD)
        val sideboardZone = ZoneKey(action.playerId, Zone.SIDEBOARD)
        // Check all exile zones since cards may be in another player's exile (Villainous Wealth)
        val exileOwner = state.turnOrder.firstOrNull { pid ->
            action.cardId in state.getZone(ZoneKey(pid, Zone.EXILE))
        }
        // The top of another player's library, under Xanathar, Guild Kingpin's permission.
        val otherLibraryOwner = if (action.cardId in state.getZone(libraryZone)) null else
            state.turnOrder.firstOrNull { pid -> action.cardId in state.getZone(ZoneKey(pid, Zone.LIBRARY)) }
        val fromZone = when {
            action.cardId in state.getZone(handZone) -> Zone.HAND
            action.cardId in state.getZone(libraryZone) || otherLibraryOwner != null -> Zone.LIBRARY
            exileOwner != null -> Zone.EXILE
            action.cardId in state.getZone(graveyardZone) -> Zone.GRAVEYARD
            action.cardId in state.getZone(sideboardZone) -> Zone.SIDEBOARD
            else -> Zone.HAND
        }
        val sourceZoneKey = if (fromZone == Zone.EXILE && exileOwner != null) {
            ZoneKey(exileOwner, Zone.EXILE)
        } else if (fromZone == Zone.LIBRARY && otherLibraryOwner != null) {
            ZoneKey(otherLibraryOwner, Zone.LIBRARY)
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

        // A land played through a `MayCastFromGraveyard(playsLands = true)` grant (Serra Paragon):
        // only when no other graveyard land permission authorizes it, so Crucible of Worlds or a
        // Muldrotha never spends the grant's allowance or attaches its rider. Read off the pre-play
        // `state`, while the card is still in the graveyard.
        val graveyardLandGrant = if (fromZone == Zone.GRAVEYARD &&
            !hasOtherGraveyardLandPermission(state, action.playerId, action.cardId)
        ) zoneResolver.graveyardLandPlayGrant(state, action.playerId, action.cardId) else null
        if (graveyardLandGrant != null && graveyardLandGrant.second.oncePerTurn) {
            // The same marker the cast path stamps, which is what makes a land play and a cast
            // share one per-turn allowance.
            newState = newState.updateEntity(graveyardLandGrant.first) { c ->
                c.with(com.wingedsheep.engine.state.components.battlefield.MayCastFromGraveyardUsedThisTurnComponent)
            }
        }

        // Record Muldrotha graveyard land permission usage
        if (fromZone == Zone.GRAVEYARD) {
            newState = recordGraveyardPlayPermissionUsage(newState, action.playerId, CardType.LAND.name)
        }
        // Mark the zone it entered from for ETB conditions (Oscorp Industries: "when this land
        // enters from a graveyard, you lose 2 life"; a land played from the top of the library or
        // from exile likewise entered from there). Lands bypass ZoneTransitionService, which
        // normally stamps this component, so set it here.
        newState = newState.updateEntity(action.cardId) { c ->
            c.with(com.wingedsheep.engine.state.components.battlefield.EnteredFromZoneComponent(fromZone))
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

        // CR 712.12 — a modal double-faced card played as a land chooses a land face *before*
        // putting it onto the battlefield, and it enters with that face up. So the face swap
        // happens here, ahead of everything that reads the permanent's characteristics: the
        // ETB-by-type record below, the static/replacement registration, the enters-tapped and
        // as-enters replacements, and the entry event's name. Choosing a face is not a transform
        // (CR 712.9 excludes modal DFCs from transforming), so this fires no transform trigger and
        // no "can't transform" effect can stop it — which is why it uses [setDfcFace] directly
        // rather than the flip helper.
        if (action.asBackFace) {
            newState = stampDoubleFacedFrontFace(newState, cardRegistry, action.cardId)
            newState = setDfcFace(
                newState, cardRegistry, action.cardId,
                com.wingedsheep.engine.state.components.identity.DoubleFacedComponent.Face.BACK
            ) ?: return ExecutionResult.error(state, "Card has no back face to play")
        }

        // A single-use grant ("play one of those cards") is spent for its whole group, from exile or
        // a graveyard alike — before the land enters the battlefield, which makes it a new object
        // and drops it from every grant (CR 400.7), so the group could no longer be found.
        if (fromZone == Zone.EXILE || fromZone == Zone.GRAVEYARD || fromZone == Zone.SIDEBOARD) {
            newState = newState.spendCappedMayPlayFor(action.cardId, action.playerId)
        }
        // A replacement that sends the entering land elsewhere instead — Containment Priest's "if a
        // nontoken creature would enter and it wasn't cast, exile it instead" catches a played land
        // creature (Dryad Arbor), since playing a land isn't casting it. Judged after the face is
        // chosen (CR 614.12: as it would exist on the battlefield). The land was still played: the
        // drop is spent and "whenever you play a land" still sees it.
        val entryRedirect = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.checkZoneChangeRedirect(
            newState, action.cardId, fromZone, Zone.BATTLEFIELD, predicateEvaluator = predicateEvaluator,
            entersAsCreature = {
                com.wingedsheep.engine.handlers.effects.EntryCharacteristics.wouldBeCreature(
                    newState, action.cardId, action.playerId, entryCopy = null, faceDown = false,
                    cardRegistry, predicateEvaluator
                )
            }
        )
        if (entryRedirect.destinationZone != Zone.BATTLEFIELD) {
            val ownerId = printedCardComponent.ownerId ?: action.playerId
            val destination = entryRedirect.destinationZone
            newState = spendExilePlayAllowances(state, newState, action, fromZone)
                .updateEntity(action.cardId) { c ->
                    c.without<ControllerComponent>()
                        .without<com.wingedsheep.engine.state.components.battlefield.EnteredFromZoneComponent>()
                }
                .addToZone(ZoneKey(ownerId, destination), action.cardId)
                .updateEntity(action.playerId) { c ->
                    c.with((c.get<LandDropsComponent>() ?: LandDropsComponent()).use())
                }
            val redirectedEvent = com.wingedsheep.engine.core.ZoneChangeEvent(
                action.cardId,
                newState.getEntity(action.cardId)?.get<CardComponent>()?.name ?: printedCardComponent.name,
                fromZone,
                destination,
                ownerId,
                oldObject = state.objectRef(action.cardId),
                newObject = newState.objectRef(action.cardId),
            )
            return ExecutionResult.success(
                newState.tick(),
                listOfNotNull(redirectedEvent, riderPlayEventFor(state, action, fromZone), landPlayedEvent)
            )
        }

        // A land creature (Dryad Arbor) is a creature entering: a granted "enters as a copy"
        // applies before it is placed, and everything below reads the copied identity.
        newState = com.wingedsheep.engine.handlers.effects.copy.GrantedEntryCopy
            .applyBeforeEntry(newState, action.cardId, cardRegistry, predicateEvaluator)
        newState = com.wingedsheep.engine.handlers.effects.BattlefieldEntry
            .place(newState, action.playerId, action.cardId)
        val enteredObject = newState.objectRef(action.cardId)

        // Lands bypass ZoneTransitionService, which is where every other zone-change path
        // stamps EnteredThisTurnComponent (cleared again at the controller's next untap step,
        // see BeginningPhaseManager). Without this, "activate only if this land entered the
        // battlefield this turn" (Hidden Lair) and any other Conditions.SourceEnteredThisTurn /
        // GameObjectFilter.enteredThisTurn() check keyed on a land would never see it as true,
        // even on the very turn it was played.
        newState = newState.updateEntity(action.cardId) { c -> c.with(EnteredThisTurnComponent) }

        // Same bypass, same consequence for CR 302.6/508.1a: ZoneTransitionService stamps
        // SummoningSicknessComponent on every permanent it puts onto the battlefield (a no-op for
        // an ordinary land, since only a {T}/{Q} cost or an attack check ever reads it, gated on
        // isCreature). A land played from hand skips that stamp entirely, which was invisible
        // until a land that's also a creature existed (Dryad Arbor) — its "{T}: Add {G}" must be
        // blocked the turn it's played, and without this it never was.
        newState = newState.updateEntity(action.cardId) { c -> c.with(SummoningSicknessComponent) }

        // Same bypass, same consequence for Rule 712 face tracking: a double-faced *land* played
        // from hand (Balamb Garden, SeeD Academy — "{5}{G}{U}, {T}: Transform this land") never went
        // through ZoneTransitionService's stamp, so it arrived with no DoubleFacedComponent and its
        // printed transform was silently a no-op. A land is always played face up (CR 305.1) and a
        // double-faced card put onto the battlefield from a zone other than the stack enters with
        // its front face up (CR 712.14), so the front face is the right one here — and the helper
        // no-ops on a single-faced land and on an entity that is already face-tracked. (CR 712.12's
        // "play a modal double-faced card as its land face" chooses the face before the entry; the
        // engine doesn't model a back-face land play yet, and if it does, that path stamps the face
        // it chose first and this call sees an already-tracked entity.)
        newState = stampDoubleFacedFrontFace(newState, cardRegistry, action.cardId)

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
            .entersUntapped(newState, action.cardId, action.playerId, predicateEvaluator = predicateEvaluator)

        // A may-play permission's entersTapped rider forces the played land tapped regardless of
        // the card's own ETB script — Lightstall Inquisitor's "each land played this way enters
        // tapped" (exile), Kaldring, the Rimestaff's "if you do, it enters tapped" (graveyard).
        // The riders come from the pre-play `state` (the grants went as the card left its zone);
        // they are matched against the land as it entered, so an MDFC is judged by its played face.
        val permissionForcesTapped = permissionLandRiders(state, action.playerId, action.cardId, fromZone)
            ?.let { newState.matchesEntersTappedRiders(action.cardId, action.playerId, it, predicateEvaluator) } == true
        if (permissionForcesTapped && !landEntersUntapped) {
            newState = newState.updateEntity(action.cardId) { c -> c.with(TappedComponent) }
        }
        // "When you play a card this way, …" rider (Fires of Mount Doom), captured before the
        // permission goes.
        val riderPlayEvent = riderPlayEventFor(state, action, fromZone)

        // The land's may-play grants went as it entered the battlefield (a new object, CR 400.7);
        // what's left is the once-per-turn bookkeeping and the linked-exile pile.
        newState = spendExilePlayAllowances(state, newState, action, fromZone)
        // CR 712.8f — a modal double-faced permanent has only the characteristics of the face
        // that's up. Everything below (the entry event's name, enters-tapped, the as-enters
        // replacements) has to read the face actually played, not the printed front.
        val cardComponent = newState.getEntity(action.cardId)?.get<CardComponent>()
            ?: printedCardComponent
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)

        // "This land enters with two charge counters on it" (the vivid lands) and the global
        // enters-with replacements other permanents carry (Doubling Season's extra counters,
        // keyword grants). Lands bypass ZoneTransitionService, so the entry counters that every
        // other permanent gets from ZoneMovementUtils / StackResolver are applied here — before the
        // tapped and as-enters branches below, since each of those is its own exit and the counters
        // belong to the entry regardless of which branch finishes it. The events are carried into
        // every exit so counter-placement triggers still see them.
        // The grant's entry rider (Serra Paragon's granted exile-and-gain-life trigger) — the land
        // half of the rider the cast path applies in PermanentEntry.
        val landRiderEvents: List<com.wingedsheep.engine.core.GameEvent> =
            graveyardLandGrant?.second?.takeIf { it.hasEntryRider }?.let { grant ->
                val (afterRider, riderEvents) = com.wingedsheep.engine.handlers.effects.EntersWithReplacements
                    .applyCastFromGraveyardRider(
                        newState, action.cardId, action.playerId,
                        grant.entersWithCounter, grant.addedSubtypeOnEntry, grant.grantedAbilityOnEntry,
                        predicateEvaluator = predicateEvaluator
                    )
                newState = afterRider
                riderEvents
            } ?: emptyList()
        val grantedReadAhead = com.wingedsheep.engine.mechanics.ReadAheadSynthesis
            .grantedChoice(newState, action.cardId, cardDef, cardRegistry, predicateEvaluator)
        val entersWithEvents: List<com.wingedsheep.engine.core.GameEvent> = landRiderEvents + (if (cardDef != null) {
            val (afterOwn, ownEvents) = com.wingedsheep.engine.handlers.effects.EntersWithReplacements
                .applyFromDefinition(newState, action.cardId, cardDef, action.playerId, predicateEvaluator = predicateEvaluator, preEntryZone = sourceZoneKey)
            val (afterGlobal, globalEvents) = com.wingedsheep.engine.handlers.effects.EntersWithReplacements
                .applyGlobal(afterOwn, action.cardId, action.playerId, cardRegistry, predicateEvaluator = predicateEvaluator, preEntryZone = sourceZoneKey)
            newState = afterGlobal
            ownEvents + globalEvents
        } else emptyList()) + run {
            // A Saga land (Urza's Saga) enters with its lore counter like any Saga (CR 714.3a);
            // the land path bypasses ZoneTransitionService, so it's placed here — unless it has
            // read ahead granted: its count is the number asked below, and that choice's resumer
            // places the counters once it is answered. An exit that never reaches that question
            // places them itself ([placeWithheldSagaEntry]).
            if (grantedReadAhead != null) return@run emptyList()
            val (afterSaga, sagaEvents) = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .applySagaEntryIfNeeded(newState, action.cardId, predicateEvaluator)
            newState = afterSaga
            sagaEvents
        }

        // A Saga land whose lore counters were withheld for a granted read-ahead question, leaving
        // through a path that won't ask it: it enters with the intrinsic single counter.
        fun placeWithheldSagaEntry(): List<com.wingedsheep.engine.core.GameEvent> {
            if (grantedReadAhead == null) return emptyList()
            val (afterSaga, sagaEvents) = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .applySagaEntryIfNeeded(newState, action.cardId, predicateEvaluator)
            newState = afterSaga
            return sagaEvents
        }

        // OnEnterRun — generic "as ~ enters, run [effect]" replacement.
        // Runs BEFORE the EntersTapped check so effects like
        // Effects.EnterTapped() (Game Trail's "otherwise" rider) apply
        // synchronously with entry. That rider consults EnterUntappedReplacements itself, so
        // returning early here doesn't skip "lands enter untapped". May pause for player input
        // via continuations.
        if (cardDef != null) {
            val onEnter = com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
                .onEnterRunEffectFor(cardDef)
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
                    cardComponent.ownerId ?: action.playerId,
                    oldObject = state.objectRef(action.cardId),
                    newObject = enteredObject,
                )
                val onEnterEvents = mutableListOf<com.wingedsheep.engine.core.GameEvent>(zoneChangeEvent)
                onEnterEvents.addAll(entersWithEvents + placeWithheldSagaEntry())
                riderPlayEvent?.let { onEnterEvents.add(it) }
                onEnterEvents.add(landPlayedEvent)
                newState = newState.tick()

                val effectContext = EffectContext(
                    // The land's entry is a step of its own, started by playing it.
                    programOrigin = com.wingedsheep.engine.core.ProgramOrigin.BuiltBy(
                        com.wingedsheep.engine.handlers.effects.composite.EffectProgramInterpreter.stepName(com.wingedsheep.sdk.scripting.OnEnterRun::class),
                        com.wingedsheep.engine.handlers.effects.composite.EffectProgramInterpreter.startedBy(PlayLandHandler::class)),
                    sourceId = action.cardId,
                    controllerId = action.playerId,
                    objectReferences = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(
                        captured = true, origin = enteredObject, source = enteredObject,
                        resolutionKey = "entry:${action.cardId}:${enteredObject?.generation}"
                    ),
                )
                val effectResult = effectExecutor(newState, onEnter.effect, effectContext)
                if (effectResult.outcome is Outcome.Paused) {
                    return ExecutionResult.propagatePause(
                        effectResult.state,
                        onEnterEvents + effectResult.events,
                    )
                }
                newState = effectResult.state
                onEnterEvents.addAll(effectResult.events)
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
                .firstOrNull { it.copyOf == null }
            if (entersAsCopy != null &&
                com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
                    .entersAsCopyCandidates(newState, action.cardId, action.playerId, entersAsCopy, predicateEvaluator = predicateEvaluator)
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
                        entryOldObject = state.objectRef(action.cardId),
                        entryNewObject = enteredObject,
                        carryEvents = listOfNotNull(riderPlayEvent, landPlayedEvent),
                        predicateEvaluator = predicateEvaluator
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

                    // The entry ZoneChangeEvent is emitted by the resumer once the life payment is
                    // settled, so the land's enters triggers are detected exactly once.
                    val events = entersWithEvents + placeWithheldSagaEntry() +
                        listOfNotNull(riderPlayEvent, landPlayedEvent)
                    newState = newState.tick()

                    val continuation = com.wingedsheep.engine.core.PayLifeOrEnterTappedLandContinuation(
                        landId = action.cardId,
                        controllerId = action.playerId,
                        lifeCost = entersTapped.payLifeCost!!,
                        fromZone = fromZone,
                        entryOldObject = state.objectRef(action.cardId),
                        entryNewObject = enteredObject,
                    )
                    return newState.suspendForDecision(
                        question = { decisionId ->
                            com.wingedsheep.engine.core.YesNoDecision(
                                id = decisionId,
                                playerId = action.playerId,
                                prompt = "Pay ${entersTapped.payLifeCost} life to have ${cardComponent.name} enter untapped?",
                                context = com.wingedsheep.engine.core.DecisionContext(
                                    sourceId = action.cardId,
                                    sourceName = cardComponent.name,
                                    phase = com.wingedsheep.engine.core.DecisionPhase.RESOLUTION
                                )
                            )
                        },
                        answer = continuation,
                        events = events
                    )
                } else {
                    val shouldEnterTapped = if (entersTapped.unlessCondition != null) {
                        // Conditional: enters tapped UNLESS condition is met
                        val context = EffectContext(
                            sourceId = action.cardId,
                            controllerId = action.playerId,
                        )
                        !conditionEvaluator.evaluate(newState, entersTapped.unlessCondition!!, context)
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
                .entersTapped(newState, action.cardId, action.playerId, predicateEvaluator = predicateEvaluator)
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
            // Granted read ahead on a land Saga (Urza's Saga under Barbara Wright).
            val firstChoice = (printedChoices + listOfNotNull(syntheticRiotChoice, grantedReadAhead))
                .sortedBy { it.choiceType.ordinal }
                .firstOrNull()
            if (firstChoice != null) {
                // Use up a land drop first
                newState = newState.updateEntity(action.playerId) { c ->
                    val landDrops = c.get<LandDropsComponent>() ?: LandDropsComponent()
                    c.with(landDrops.use())
                }

                // The entry ZoneChangeEvent is emitted by the resumer once the choice is recorded
                // (see PermanentEntryReplacements), so the land's enters triggers see the choice
                // and are detected exactly once.
                val events = entersWithEvents + listOfNotNull(riderPlayEvent, landPlayedEvent)
                newState = newState.tick()

                // Build the choice prompt + entity-keyed continuation via the shared on-battlefield
                // entry helper (also used by Momir token minting). The land is already on the
                // battlefield; the resumer records the choice and emits the entry event afterward.
                val result = com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
                    .pauseForEntersWithChoice(
                        state = newState,
                        entityId = action.cardId,
                        controllerId = action.playerId,
                        cardComponent = cardComponent,
                        choice = firstChoice,
                        fromZone = fromZone,
                        entryOldObject = state.objectRef(action.cardId),
                        entryNewObject = enteredObject,
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
            cardComponent.ownerId ?: action.playerId,
            oldObject = state.objectRef(action.cardId),
            newObject = enteredObject,
        )

        val events = listOf(zoneChangeEvent) + entersWithEvents + listOfNotNull(riderPlayEvent, landPlayedEvent)
        newState = newState.tick()
        return ExecutionResult.success(newState, events)
    }

    private fun isOnTopOfLibraryWithPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        // A play-from-top permission — Gwenom's over the player's own library, Xanathar, Guild
        // Kingpin's over a target opponent's — covers lands as well as spells ("play").
        if (zoneResolver.topOfLibraryAlternativeGrant(state, playerId, cardId) != null) return true
        val library = state.getLibrary(playerId)
        if (library.isEmpty() || library.first() != cardId) return false
        return hasPlayFromTopOfLibrary(state, playerId, cardId)
    }

    private fun isInLibraryWithPlayPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean = cardId in state.getLibrary(playerId) &&
        state.activeMayPlayFor(cardId, playerId, conditionEvaluator, cardRegistry).any { !it.nonLandOnly }

    /**
     * True when [cardId] is in [playerId]'s own sideboard — outside the game (CR 400.11a) — and a
     * stored may-play permission lets them play it ("You may play a card you own from outside the
     * game this turn", Wish). Only the owner's sideboard: the permission says "a card you own".
     */
    private fun isInSideboardWithPlayPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean = cardId in state.getZone(ZoneKey(playerId, Zone.SIDEBOARD)) &&
        state.activeMayPlayFor(cardId, playerId, conditionEvaluator, cardRegistry).any { !it.nonLandOnly }

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
     * The `entersTapped` riders of the may-play permissions the land is played through (see
     * [mayPlayEntersTappedRiders]), or null when a rider-free route could play it instead: a
     * linked-exile grant for an exiled land, Crucible / Muldrotha / Mayhem / a shared graveyard
     * grant for one in the graveyard. Read from [state] *before* the card left its zone, since its
     * grants are dropped as it moves to the battlefield.
     */
    private fun permissionLandRiders(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId,
        fromZone: Zone,
    ): List<com.wingedsheep.sdk.scripting.GameObjectFilter>? {
        val riders = state.mayPlayEntersTappedRiders(cardId, playerId, conditionEvaluator, cardRegistry) { !it.nonLandOnly }
            ?: return null
        val riderFreeRoute = when (fromZone) {
            Zone.EXILE -> com.wingedsheep.engine.handlers.effects.linkedexile.LinkedExilePlayUtils
                .canPlayLand(state, playerId, cardId, legality)
            Zone.GRAVEYARD -> hasNonPermissionGraveyardLandPlay(state, playerId, cardId) ||
                zoneResolver.graveyardLandPlayGrant(state, playerId, cardId) != null
            else -> true
        }
        return riders.takeUnless { riderFreeRoute }
    }

    private fun hasPlayFromTopOfLibrary(state: GameState, playerId: EntityId, cardId: EntityId): Boolean {
        val landCard = state.getEntity(cardId)?.get<CardComponent>() ?: return false
        for (entityId in state.getBattlefield(playerId)) {
            val cardDef = state.getEntity(entityId)?.faceUpCardDefinition(cardRegistry) ?: continue
            for (ability in cardDef.liveStaticAbilities(state, entityId)) {
                // Honor a conditional gate (e.g. The Lunar Whale's "as long as it attacked this
                // turn") against the granting permanent before allowing the land play from top.
                val unwrapped = if (ability is ConditionalStaticAbility) {
                    if (!evaluateStaticGate(state, ability.condition, entityId, playerId)) continue
                    ability.ability
                } else ability
                if (unwrapped is PlayFromTopOfLibrary) return true
                // Isu the Abominable: only lands matching the permission's land filter (snow lands).
                if (unwrapped is PlayLandsAndCastFilteredFromTopOfLibrary && unwrapped.allowsLand(landCard)) {
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
        if (hasOtherGraveyardLandPermission(state, playerId, cardId)) return true
        // Serra Paragon's shared "play a land or cast a permanent spell" grant.
        return zoneResolver.graveyardLandPlayGrant(state, playerId, cardId) != null
    }

    /**
     * Every graveyard land permission *other than* a `MayCastFromGraveyard(playsLands = true)`
     * grant — those are asked last, so a play these already authorize never spends that grant's
     * per-turn allowance nor takes its rider. [cardId] is known to be in [playerId]'s graveyard.
     */
    private fun hasOtherGraveyardLandPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        // Generic per-card may-play permission (e.g. Tablet of Discovery's "mill a card. You
        // may play that card this turn." — the milled card sits in the graveyard). Mirrors the
        // exile path in [isInExileWithPlayPermission] and CastFromZoneEnumerator, which already
        // offers the PlayLand action for such a card; without this the handler would reject the
        // very action the enumerator advertised.
        // A nonLandOnly permission ("you may cast that card" wording) never authorizes playing
        // a land — mirrors isInExileWithPlayPermission.
        if (state.activeMayPlayFor(cardId, playerId, conditionEvaluator, cardRegistry)
                .any { !it.nonLandOnly }) return true
        return hasNonPermissionGraveyardLandPlay(state, playerId, cardId)
    }


    /**
     * "When you play a card this way, …" (Fires of Mount Doom): the event a land played from exile
     * through a rider-carrying may-play permission raises. Read off the pre-play [state].
     */
    private fun riderPlayEventFor(
        state: GameState,
        action: PlayLand,
        fromZone: Zone
    ): com.wingedsheep.engine.core.CardPlayedFromPermissionEvent? {
        if (fromZone != Zone.EXILE) return null
        return state.mayPlayPermissions.firstOrNull { permission ->
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
    }

    /**
     * A land played from exile spends the once-per-turn allowance it was played through (Hauken's
     * Insight, Evelyn) and leaves any linked-exile pile. Granters are resolved against the pre-play
     * [state]; the result builds on [newState].
     */
    private fun spendExilePlayAllowances(
        state: GameState,
        newState: GameState,
        action: PlayLand,
        fromZone: Zone
    ): GameState {
        if (fromZone != Zone.EXILE) return newState
        var result = newState
        // Record once-per-turn linked-exile permission usage (Hauken's Insight — "Once during
        // each of your turns, you may play a land or cast a spell"). Resolved against the
        // pre-play `state`: the unlink below removes the card from its granter's pile, after
        // which nothing connects the two. The marker is the same one `CastSpellHandler`
        // stamps, which is what makes the land and the spell share one allowance.
        val linkedGranter = com.wingedsheep.engine.handlers.effects.linkedexile.LinkedExilePlayUtils
            .landGranterFor(state, action.playerId, action.cardId, legality)
        if (linkedGranter?.ability?.oncePerTurn == true) {
            result = result.updateEntity(linkedGranter.sourceId) { c ->
                c.with(
                    com.wingedsheep.engine.state.components.battlefield
                        .MayCastFromLinkedExileUsedThisTurnComponent
                )
            }
        }
        // The filter-defined sibling (Evelyn, the Covetous): a land play spends the same
        // once-each-turn allowance a cast would.
        state.oncePerTurnGranterToSpend(action.cardId, action.playerId, conditionEvaluator, cardRegistry)
            ?.let { granter ->
                result = result.updateEntity(granter) { c ->
                    c.with(
                        com.wingedsheep.engine.state.components.battlefield
                            .MayCastFromLinkedExileUsedThisTurnComponent
                    )
                }
            }
        // Drop the card from any linked-exile granter (Valgavoth) now that it has left exile,
        // so the granter no longer lists it. (Lands bypass ZoneTransitionService, which would
        // otherwise unlink on the way out.)
        result = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
            .unlinkFromAllLinkedExiles(result, action.cardId)
        return result
    }
    /**
     * The graveyard land permissions that aren't a per-card may-play grant: Crucible-style
     * statics and emblems, Muldrotha's land allowance, and Mayhem.
     */
    private fun hasNonPermissionGraveyardLandPlay(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
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
     * Returns true if the player controls a permanent — or has an emblem — with
     * [MayPlayLandsFromGraveyard] (Crucible of Worlds style — no per-turn usage tracking needed).
     */
    private fun hasLandGraveyardPlayPermission(state: GameState, playerId: EntityId): Boolean {
        for (entityId in state.getBattlefield(playerId)) {
            val cardDef = state.getEntity(entityId)?.faceUpCardDefinition(cardRegistry) ?: continue
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
        // An emblem the player has (Wrenn and Realmbreaker's −7) grants it from outside every zone.
        return state.emblemStaticAbilitiesOf(playerId).any { (_, ability) -> ability is MayPlayLandsFromGraveyard }
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
            val cardDef = state.getEntity(entityId)?.faceUpCardDefinition(cardRegistry) ?: continue
            if (cardDef.liveStaticAbilities(state, entityId).any { it is MayPlayPermanentsFromGraveyard }) {
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
                services.conditionEvaluator,
                services.effectExecutorRegistry::execute,
                services.legalityKernel
            )
        }
    }
}
