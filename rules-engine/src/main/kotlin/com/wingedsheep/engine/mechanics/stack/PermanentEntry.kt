package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EntersWithReplacements
import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.engine.mechanics.daynight.DayNightService
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CastFromHandComponent
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.WarpedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.engine.state.permissions.removeMayPlayPermissionsForCard
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.MoveTrackedBattlefieldObjectEffect
import com.wingedsheep.sdk.scripting.effects.WarpExileEffect
import com.wingedsheep.sdk.scripting.targets.*

/**
 * Puts a resolving permanent spell onto the battlefield (CR 608.3): turns the stack object into a
 * permanent carrying its cast-time record, applies its enters-with replacements, and registers the
 * delayed triggers its alternative cost promised.
 */
internal class PermanentEntry(
    private val cardRegistry: CardRegistry,
    private val staticAbilityHandler: StaticAbilityHandler,
    private val conditionEvaluator: ConditionEvaluator
) {
    /**
     * Complete the permanent entry to the battlefield (shared between normal resolution and clone continuation).
     *
     * The stack object becomes the permanent (CR 608.3), in stages: it takes on its permanent
     * components and cast-time record ([becomePermanent]), its own and global "enters tapped" /
     * "enters with" replacements apply (CR 614.1c), it takes on its entry designations (Class level,
     * DFC face, Saga lore), and only then is it placed on the battlefield — after which the entry
     * riders that need a real permanent (sneak, Room doors, warp / dash delayed triggers, prepared)
     * run. The [ZoneChangeEvent] leads the returned events.
     */
    internal fun enterPermanentOnBattlefield(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?
    ): Pair<GameState, List<GameEvent>> {
        val controllerId = spellComponent.casterId
        // The Tomb of Aclazotz: a graveyard-cast entry rider frozen on this spell at cast time.
        // Captured before the `updateEntity` block below strips it; applied on entry further down.
        val graveyardCastRider =
            state.getEntity(spellId)?.get<com.wingedsheep.engine.state.components.stack.GraveyardCastRiderComponent>()

        // For Auras: get the target before removing TargetsComponent. The target is usually a
        // permanent, but "enchant player" Auras (Grievous Wound) attach to a player — both are
        // entities, so AttachedToComponent holds either id (CR 303.4).
        val auraTargetId = if (cardComponent?.isAura == true) {
            state.getEntity(spellId)?.get<TargetsComponent>()?.targets?.firstOrNull()?.let { target ->
                when (target) {
                    is ChosenTarget.Permanent -> target.entityId
                    is ChosenTarget.Player -> target.playerId
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
            becomePermanent(state, c, spellId, spellComponent, cardDef, controllerId, resolvingAsSpellCopy, auraTargetId)
        }

        newState = countFaceDownEntry(newState, spellComponent, controllerId)
        newState = scheduleSpellCopyTokenSacrifice(newState, spellId, controllerId)
        newState = registerAuraOnHost(newState, spellId, auraTargetId)
        newState = applyOwnEntersTapped(newState, spellId, spellComponent, cardDef, controllerId)

        // Handle "enters with counters" replacement effects (before adding to battlefield)
        val counterEvents = mutableListOf<GameEvent>()

        // CR 603.2f — an Aura entering attached to its enchant target "becomes attached"; emit the
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

        newState = applyEntryCounterReplacements(
            newState, spellId, spellComponent, cardComponent, cardDef, controllerId, graveyardCastRider, counterEvents
        )
        newState = applyEntryDesignations(newState, spellId, spellComponent, cardDef)

        // Add to battlefield — clean up any may-play permission first (mirrors the same
        // cleanup done in resolveNonPermanentSpell before the card goes to the graveyard).
        newState = newState.removeMayPlayPermissionsForCard(spellId)
        newState = com.wingedsheep.engine.handlers.effects.BattlefieldEntry
            .place(newState, controllerId, spellId)

        newState = applyGlobalEntersTapped(newState, spellId, spellComponent, cardDef, controllerId)
        newState = enterSneakAttacking(newState, spellId, spellComponent, controllerId)
        addCastFaceDoorUnlockedEvents(newState, spellId, cardComponent, controllerId, counterEvents)
        newState = scheduleWarpExile(newState, spellId, spellComponent, cardComponent, controllerId)
        newState = scheduleDashReturn(newState, spellId, spellComponent, cardComponent, controllerId)
        newState = enterPreparedIfKeyworded(newState, spellId, spellComponent, cardDef, controllerId)

        // Entry precedes the counters placed on that battlefield object.
        counterEvents.add(0, ZoneChangeEvent(
            spellId, nameVisibleToAll(newState, spellId, cardComponent?.name ?: "Unknown"),
            Zone.STACK, Zone.BATTLEFIELD, cardComponent?.ownerId ?: controllerId,
            xValue = spellComponent.xValue,
            enteredBattlefieldTimestamp = newState.getEntity(spellId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent>()?.timestamp,
            oldObject = state.objectRef(spellId), newObject = newState.objectRef(spellId),
        ))
        return newState to counterEvents
    }

    // -------------------------------------------------------------------------
    // Becoming a permanent: the components the stack object trades its spell components for
    // -------------------------------------------------------------------------

    /**
     * Update the resolving stack object [c] into a permanent: remove the spell components, add the
     * permanent components, and carry over everything the permanent remembers about how it was cast.
     * [state] is the pre-entry state the face-down cast mode is read from.
     */
    private fun becomePermanent(
        state: GameState,
        c: ComponentContainer,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?,
        controllerId: EntityId,
        resolvingAsSpellCopy: Boolean,
        auraTargetId: EntityId?
    ): ComponentContainer {
        var updated = c.without<SpellOnStackComponent>()
            .without<TargetsComponent>()
            .without<com.wingedsheep.engine.state.components.stack.GraveyardCastRiderComponent>()
            .with(ControllerComponent(controllerId))

        if (resolvingAsSpellCopy) {
            updated = updated.with(TokenComponent)
        }

        // If cast face-down (morph / disguise), add FaceDownComponent and strip any
        // RevealedToComponent from hand-peek effects (zone change = new object).
        // MorphDataComponent was already added when the spell was cast; the mode marker is
        // what carries disguise's ward {2} (CR 702.168a) and the face-down art.
        if (spellComponent.castFaceDown) {
            updated = updated.with(FaceDownComponent)
                .without<RevealedToComponent>()
            val castDef = state.getEntity(spellId)?.get<CardComponent>()
                ?.let { cardRegistry.getCard(it.cardDefinitionId) }
            FaceDownTurnUp.castMode(castDef)?.let { updated = updated.with(FaceDownModeComponent(it)) }
        }

        // All permanents enter summoning sick (CR 302.6 / 508.1a — the control-continuity
        // check is about the permanent, not whether it was a creature the whole turn). Vehicles
        // and animated lands that become creatures mid-turn must inherit the marker too.
        // Downstream checks gate on isCreature/{T}-cost so this is harmless for lands and
        // non-creature artifacts until they become creatures (Crew, animate-land, etc.).
        updated = updated.with(SummoningSicknessComponent)

        // Track that this permanent entered the battlefield this turn
        updated = updated.with(EnteredThisTurnComponent)

        updated = withCastOriginMarkers(updated, spellComponent)
        updated = withCastChoices(updated, spellComponent)
        updated = withAlternativeCostMarkers(updated, spellComponent, cardDef)
        updated = withRoomDoors(updated, spellComponent, cardDef)
        updated = withCastRecord(updated, spellComponent)

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

        // CR 707.10f token-copy riders: a copy of a permanent spell that carried added keywords
        // (e.g. "the copy gains haste", Choreographed Sparks) bakes them onto the resulting
        // token's base keywords for its whole life on the battlefield.
        val copyRiders = updated.get<com.wingedsheep.engine.state.components.stack.SpellCopyTokenRidersComponent>()
        if (copyRiders != null && copyRiders.addedKeywords.isNotEmpty()) {
            val card = updated.get<CardComponent>()
            if (card != null) {
                updated = updated.with(card.copy(baseKeywords = card.baseKeywords + copyRiders.addedKeywords))
            }
        }

        return updated
    }

    /** The zone the permanent was cast from, for "if it was cast from …" payoffs. */
    private fun withCastOriginMarkers(
        container: ComponentContainer,
        spellComponent: SpellOnStackComponent
    ): ComponentContainer {
        var updated = container
        // Track if this permanent was cast from hand (for cards like Phage the Untouchable)
        if (spellComponent.castFromZone == Zone.HAND) {
            updated = updated.with(CastFromHandComponent)
        }

        // Track if this permanent was cast from a graveyard (for triggers that care about
        // creatures cast from graveyard — e.g., Twilight Diviner).
        if (spellComponent.castFromZone == Zone.GRAVEYARD) {
            updated = updated.with(com.wingedsheep.engine.state.components.battlefield.CastFromGraveyardComponent)
        }

        // Track if this permanent was cast from a library (e.g. "cast from the top of your
        // library" permissions — Mikey & Don's +1/+1 rider on creatures cast this way).
        if (spellComponent.castFromZone == Zone.LIBRARY) {
            updated = updated.with(com.wingedsheep.engine.state.components.battlefield.CastFromLibraryComponent)
        }

        // Track if this permanent was cast from exile (impulse draws, plot/foretell, an
        // adventurer's permanent half, linked-exile grants) — Extraordinary Journey.
        if (spellComponent.castFromZone == Zone.EXILE) {
            updated = updated.with(com.wingedsheep.engine.state.components.battlefield.CastFromExileComponent)
        }
        return updated
    }

    private fun withCastChoices(
        container: ComponentContainer,
        spellComponent: SpellOnStackComponent
    ): ComponentContainer {
        var updated = container
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
        val entered = updated.get<com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent>()
        var bag = entered ?: com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent()
        spellComponent.xValue?.let { bag = bag.copy(x = it) }
        // The optional additional cost declared while casting (kicker → KICKED, bargain →
        // BARGAINED, CR 702.166b) marks the permanent under its own slot, so a bargained
        // permanent's "if it was bargained" enters trigger reads true while a kicker payoff
        // reading KICKED still reads false.
        spellComponent.declaredCostSlot?.let { slot ->
            bag = bag.withChoice(
                slot,
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
        // Web-slinging (CR 702.188): durably mark the permanent so Conditions.WebSlungCostWasPaid
        // reads "it was cast using web-slinging" for its whole life, and carry the returned
        // creature's mana value (CR 118.9c) so a rider like Scarlet Spider, Ben Reilly can enter
        // with that many +1/+1 counters via DynamicAmount.CastChoice(WEB_SLUNG_RETURNED_MV).
        if (spellComponent.wasWebSlung) {
            bag = bag.withChoice(
                com.wingedsheep.sdk.scripting.ChoiceSlot.WEB_SLUNG,
                com.wingedsheep.engine.state.components.battlefield.ChoiceValue.Flag
            ).withChoice(
                com.wingedsheep.sdk.scripting.ChoiceSlot.WEB_SLUNG_RETURNED_MV,
                com.wingedsheep.engine.state.components.battlefield.ChoiceValue.NumberChoice(
                    spellComponent.webSlungReturnedManaValue
                )
            )
        }
        // Mayhem (CR 702.187): durably mark a permanent cast from the graveyard for its
        // mayhem cost so Conditions.MayhemCostWasPaid reads it for the permanent's whole
        // life. (Note: mayhem does NOT exile the spell on resolution — a permanent just
        // enters the battlefield here via the normal permanent-resolution path.)
        if (spellComponent.wasMayhem) {
            bag = bag.withChoice(
                com.wingedsheep.sdk.scripting.ChoiceSlot.MAYHEM_CAST,
                com.wingedsheep.engine.state.components.battlefield.ChoiceValue.Flag
            )
        }
        // Waterbend (Avatar): durably mark a permanent cast with its (optional) waterbend
        // cost paid so Conditions.WaterbendWasPaid reads it for the permanent's whole life.
        if (spellComponent.wasWaterbendPaid) {
            bag = bag.withChoice(
                com.wingedsheep.sdk.scripting.ChoiceSlot.WATERBEND_PAID,
                com.wingedsheep.engine.state.components.battlefield.ChoiceValue.Flag
            )
        }
        // Gift (CR 702.174a–b): the promise was elected as an additional cost while casting,
        // so the permanent carries both the flag and the promised opponent durably. Its gift
        // trigger ("when this permanent enters, if its gift cost was paid, …") and any
        // "if the gift was(n't) promised" rider read them back through
        // Conditions.GiftWasPromised / Player.ChosenOpponent — no resolution-time question.
        spellComponent.giftRecipient?.let { recipient ->
            bag = bag.withChoice(
                com.wingedsheep.sdk.scripting.ChoiceSlot.GIFT_PROMISED,
                com.wingedsheep.engine.state.components.battlefield.ChoiceValue.Flag
            ).withChoice(
                com.wingedsheep.sdk.scripting.ChoiceSlot.OPPONENT,
                com.wingedsheep.engine.state.components.battlefield.ChoiceValue.EntityChoice(recipient)
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
        return updated
    }

    /** Markers for the alternative cost the permanent was cast for — warp, dash, evoke, impending. */
    private fun withAlternativeCostMarkers(
        container: ComponentContainer,
        spellComponent: SpellOnStackComponent,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?
    ): ComponentContainer {
        var updated = container
        // Track if this permanent was cast for its warp cost
        if (spellComponent.wasWarped) {
            updated = updated.with(WarpedComponent)
        }

        // Track if this permanent was cast for its dash cost (CR 702.109a — grants haste
        // live off this marker; see DashedComponent's doc).
        if (spellComponent.wasDashed) {
            updated = updated.with(com.wingedsheep.engine.state.components.battlefield.DashedComponent)
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
        return updated
    }

    private fun withRoomDoors(
        container: ComponentContainer,
        spellComponent: SpellOnStackComponent,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?
    ): ComponentContainer {
        var updated = container
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
        return updated
    }

    private fun withCastRecord(
        container: ComponentContainer,
        spellComponent: SpellOnStackComponent
    ): ComponentContainer {
        var updated = container
        // Record mana colors and provenance spent to cast (for mana-spent-gated triggers and
        // enters-the-battlefield "for each mana from a [subtype] spent to cast it" payoffs).
        if (spellComponent.manaSpentWhite > 0 || spellComponent.manaSpentBlue > 0 ||
            spellComponent.manaSpentBlack > 0 || spellComponent.manaSpentRed > 0 ||
            spellComponent.manaSpentGreen > 0 || spellComponent.manaSpentColorless > 0 ||
            spellComponent.manaSpentBySubtype.isNotEmpty()) {
            updated = updated.with(com.wingedsheep.engine.state.components.battlefield.CastRecordComponent(
                whiteSpent = spellComponent.manaSpentWhite,
                blueSpent = spellComponent.manaSpentBlue,
                blackSpent = spellComponent.manaSpentBlack,
                redSpent = spellComponent.manaSpentRed,
                greenSpent = spellComponent.manaSpentGreen,
                colorlessSpent = spellComponent.manaSpentColorless,
                manaSpentBySubtype = spellComponent.manaSpentBySubtype
            ))
        }
        return updated
    }

    // -------------------------------------------------------------------------
    // Before the permanent is placed: trackers, attachment, enters-tapped and enters-with
    // -------------------------------------------------------------------------

    private fun countFaceDownEntry(
        state: GameState,
        spellComponent: SpellOnStackComponent,
        controllerId: EntityId
    ): GameState {
        var newState = state
        // "A face-down creature entered the battlefield under your control this turn" (Tunnel
        // Tipster, Oblivious Bookworm). The per-player counter is also bumped by the
        // MoveToZone/MoveCollection face-down paths (manifest, cloak) in ZoneTransitionService;
        // a morph/disguise *cast* resolves through here instead and never touches that service,
        // so without this the tracker would miss the most common face-down entry of all.
        // Cleared at the turn boundary by CleanupPhaseManager.
        if (spellComponent.castFaceDown) {
            newState = newState.updateEntity(controllerId) { playerContainer ->
                val existing = playerContainer
                    .get<com.wingedsheep.engine.state.components.player.PermanentEnteredFaceDownThisTurnComponent>()
                    ?: com.wingedsheep.engine.state.components.player.PermanentEnteredFaceDownThisTurnComponent()
                playerContainer.with(
                    com.wingedsheep.engine.state.components.player
                        .PermanentEnteredFaceDownThisTurnComponent(existing.count + 1)
                )
            }
        }
        return newState
    }

    private fun scheduleSpellCopyTokenSacrifice(
        state: GameState,
        spellId: EntityId,
        controllerId: EntityId
    ): GameState {
        var newState = state
        // CR 707.10f token-copy riders: register the delayed "sacrifice this token" trigger after
        // the permanent enters. The token shares the resolving spell-copy's entity id, so the
        // delayed trigger targets `spellId` directly.
        val copyRiders = newState.getEntity(spellId)
            ?.get<com.wingedsheep.engine.state.components.stack.SpellCopyTokenRidersComponent>()
        val sacrificeStep = copyRiders?.sacrificeAtStep
        if (sacrificeStep != null) {
            val sourceName = newState.getEntity(spellId)?.get<CardComponent>()?.name ?: "Unknown"
            val (triggerId, allocatedState) = newState.newRoutingId()
            newState = allocatedState.addDelayedTrigger(
                DelayedTriggeredAbility(
                    id = triggerId,
                    effect = com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect(
                        com.wingedsheep.sdk.scripting.targets.EffectTarget.SpecificEntity(spellId)
                    ),
                    fireAtStep = sacrificeStep,
                    sourceId = spellId,
                    objectReferences = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(captured = true,
                        origin = newState.objectRef(spellId), source = newState.objectRef(spellId)),
                    sourceName = sourceName,
                    controllerId = controllerId,
                    fireOnPlayerId = if (copyRiders.sacrificeOnlyOnControllersTurn) controllerId else null
                )
            )
            // The rider component has done its job; strip it so it doesn't linger on the permanent.
            newState = newState.updateEntity(spellId) { c ->
                c.without<com.wingedsheep.engine.state.components.stack.SpellCopyTokenRidersComponent>()
            }
        }
        return newState
    }

    private fun registerAuraOnHost(state: GameState, spellId: EntityId, auraTargetId: EntityId?): GameState {
        var newState = state
        // Aura: add reverse AttachmentsComponent on the enchanted permanent
        if (auraTargetId != null) {
            newState = newState.updateEntity(auraTargetId) { container ->
                val existing = container.get<com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent>()
                val updatedIds = (existing?.attachedIds ?: emptyList()) + spellId
                container.with(com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent(updatedIds))
            }
        }
        return newState
    }

    private fun applyOwnEntersTapped(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?,
        controllerId: EntityId
    ): GameState {
        var newState = state
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
                    !conditionEvaluator.evaluate(
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
        return newState
    }

    /**
     * The counters the permanent enters with (CR 614.1c): its own and global enters-with
     * replacements, a graveyard-cast rider, and a planeswalker's or battle's intrinsic entry
     * counters. Appends the resulting events to [counterEvents].
     */
    private fun applyEntryCounterReplacements(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?,
        controllerId: EntityId,
        graveyardCastRider: com.wingedsheep.engine.state.components.stack.GraveyardCastRiderComponent?,
        counterEvents: MutableList<GameEvent>
    ): GameState {
        var newState = state
        if (cardDef != null && !spellComponent.castFaceDown) {
            val totalManaSpent = spellComponent.manaSpentWhite + spellComponent.manaSpentBlue +
                spellComponent.manaSpentBlack + spellComponent.manaSpentRed +
                spellComponent.manaSpentGreen + spellComponent.manaSpentColorless
            val (counterState, events) = applyEntersWithReplacements(
                newState, spellId, cardDef, controllerId, spellComponent.xValue, totalManaSpent
            )
            newState = counterState
            counterEvents.addAll(events)
        }

        // The Tomb of Aclazotz cast-this-way entry rider: a creature cast from the graveyard under
        // its grant enters with a finality counter and gains "Vampire" in addition to its other
        // types. Applied after the printed enters-with replacements so both stack cleanly (CR 614).
        if (graveyardCastRider != null && !spellComponent.castFaceDown) {
            val (riderState, riderEvents) =
                com.wingedsheep.engine.handlers.effects.EntersWithReplacements.applyCastFromGraveyardRider(
                    newState, spellId, controllerId,
                    graveyardCastRider.entersWithCounter, graveyardCastRider.addedSubtype
                )
            newState = riderState
            counterEvents.addAll(riderEvents)
        }

        // Counters bought while casting (Chorus of the Conclave: "that creature enters with that
        // many additional +1/+1 counters on it"). Recorded on the spell when the cost was paid, so
        // they arrive even if the granting permanent has since left the battlefield. Placed through
        // the shared entry-counter path so counter-modifying replacements see them.
        val boughtCounters = spellComponent.additionalEntryCounters
        if (boughtCounters != null && boughtCounters.count > 0) {
            val (boughtState, boughtEvents) = com.wingedsheep.engine.handlers.effects.EntersWithReplacements.placeEntryCounters(
                newState, spellId,
                boughtCounters.counterType, boughtCounters.count,
                controllerId, cardComponent?.name ?: "",
                predicateEvaluator = conditionEvaluator.predicates
            )
            newState = boughtState
            counterEvents.addAll(boughtEvents)
        }

        // Handle the intrinsic entry counters of a planeswalker (starting loyalty, CR 306.5b) or a
        // battle (printed defense, CR 310.4b). This is the cast pipeline's entry point for those
        // intrinsic entry replacements — it runs here, while the permanent is still on the stack,
        // because resolution places permanents via addToZone rather than
        // ZoneTransitionService.moveToZone. Every other entry reaches the same shared
        // placeEntryCounters call through ZoneMovementUtils.applyIntrinsicEntryCountersIfNeeded.
        val intrinsicEntryCounters = if (cardDef != null && !spellComponent.castFaceDown) {
            when {
                cardDef.startingLoyalty != null ->
                    CounterType.LOYALTY to cardDef.startingLoyalty!!
                cardDef.startingDefense != null ->
                    com.wingedsheep.engine.mechanics.battle.Battles.DEFENSE_COUNTER to cardDef.startingDefense!!
                else -> null
            }
        } else null
        if (intrinsicEntryCounters != null) {
            val (entryCounterState, entryCounterEvents) = EntersWithReplacements.placeEntryCounters(
                newState, spellId, intrinsicEntryCounters.first, intrinsicEntryCounters.second,
                controllerId, cardComponent?.name ?: "",
                predicateEvaluator = conditionEvaluator.predicates
            )
            newState = entryCounterState
            counterEvents.addAll(entryCounterEvents)
        }
        return newState
    }

    /** The designations a Class, a double-faced card and a Saga enter with. */
    private fun applyEntryDesignations(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?
    ): GameState {
        var newState = state
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

            newState = DayNightService.applyDayboundEntry(newState, cardRegistry, spellId)
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
        return newState
    }

    // -------------------------------------------------------------------------
    // After the permanent is placed: riders that need a real permanent
    // -------------------------------------------------------------------------

    private fun applyGlobalEntersTapped(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?,
        controllerId: EntityId
    ): GameState {
        var newState = state
        // Global "[filter] enter tapped" replacements sourced from OTHER battlefield permanents
        // (Authority of the Consuls — "Creatures your opponents control enter tapped"). The
        // self-only EntersTapped handled earlier covers a permanent's own printed clause; a
        // permanent cast normally must ALSO be tapped by another permanent's global
        // PermanentsEnterTapped, matching the PlayLand (PlayLandHandler) and moveToZone /
        // reanimation (ZoneTransitionService) paths that already consult it. Checked after the
        // entity is on the battlefield so its controller/type resolve for the filter. CR 614: an
        // applicable "enters untapped" replacement still wins, and a self-EntersTapped that already
        // tapped it stands. Sneak sets its own tapped-and-attacking state below, so skip it here.
        if (cardDef != null && !spellComponent.castFaceDown && !spellComponent.wasSneaked) {
            val alreadyTapped = newState.getEntity(spellId)?.has<TappedComponent>() == true
            val entersUntapped = com.wingedsheep.engine.handlers.effects.EnterUntappedReplacements
                .entersUntapped(newState, spellId, controllerId, predicateEvaluator = conditionEvaluator.predicates)
            if (!alreadyTapped && !entersUntapped &&
                com.wingedsheep.engine.handlers.effects.EnterTappedReplacements
                    .entersTapped(newState, spellId, controllerId, predicateEvaluator = conditionEvaluator.predicates)
            ) {
                newState = newState.updateEntity(spellId) { c -> c.with(TappedComponent) }
            }
        }
        return newState
    }

    private fun enterSneakAttacking(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        controllerId: EntityId
    ): GameState {
        var newState = state
        // Sneak (CR 702.190b / 506.3a): a permanent spell whose sneak cost was paid enters
        // tapped and attacking the same player, planeswalker, or battle the returned unblocked
        // creature was attacking. A non-creature permanent can't attack, so it just enters tapped
        // (506.3a).
        if (spellComponent.wasSneaked) {
            newState = newState.updateEntity(spellId) { c -> c.with(TappedComponent) }
            val projected = newState.projectedState
            // CR 506.3c / 508.4a: the creature only enters attacking if the carried defender is
            // still a legal attack target — an opponent still in the game, an opponent's
            // planeswalker still on the battlefield, or a battle still on the battlefield and
            // protected by an opponent (mirrors the defender check in AttackPhaseManager). If it's
            // no longer valid, the creature enters but is never attacking — no redirect.
            val opponents = newState.getOpponents(controllerId).toSet()
            val legalDefender = spellComponent.sneakAttackDefenderId?.takeIf { d ->
                (d in newState.turnOrder && d != controllerId) ||
                    (projected.isPlaneswalker(d) &&
                        d in newState.getBattlefield() &&
                        projected.getController(d) != controllerId) ||
                    (projected.isBattle(d) &&
                        d in newState.getBattlefield() &&
                        com.wingedsheep.engine.mechanics.battle.Battles.canBeAttackedBy(newState, d, controllerId, opponents))
            }
            if (legalDefender != null && projected.isCreature(spellId)) {
                newState = newState.updateEntity(spellId) { c ->
                    c.with(AttackingComponent(legalDefender))
                }
                newState = com.wingedsheep.engine.mechanics.combat.AttackedPermanents.markAttacked(newState, legalDefender)
            }
        }
        return newState
    }

    private fun addCastFaceDoorUnlockedEvents(
        newState: GameState,
        spellId: EntityId,
        cardComponent: CardComponent?,
        controllerId: EntityId,
        counterEvents: MutableList<GameEvent>
    ) {
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
    }

    private fun scheduleWarpExile(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?,
        controllerId: EntityId
    ): GameState {
        var newState = state
        // Warp: create delayed trigger to exile at beginning of next end step. Snapshot the
        // permanent's battlefield-entry timestamp so the exile only affects this battlefield
        // object — if the permanent leaves and re-enters before the trigger resolves (blink),
        // it's a new object the delayed trigger no longer tracks (CR 603.7c / 400.7).
        if (spellComponent.wasWarped) {
            val entryTimestamp = newState.getEntity(spellId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent>()
                ?.timestamp
            val (triggerId, allocatedState) = newState.newRoutingId()
            val delayedTrigger = DelayedTriggeredAbility(
                id = triggerId,
                effect = WarpExileEffect(
                    target = EffectTarget.SpecificEntity(spellId),
                    enteredBattlefieldTimestamp = entryTimestamp
                ),
                fireAtStep = Step.END,
                sourceId = spellId,
                        objectReferences = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(captured = true,
                            origin = newState.objectRef(spellId), source = newState.objectRef(spellId)),
                sourceName = cardComponent?.name ?: "Unknown",
                controllerId = controllerId
            )
            newState = allocatedState.addDelayedTrigger(delayedTrigger)
        }
        return newState
    }

    private fun scheduleDashReturn(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?,
        controllerId: EntityId
    ): GameState {
        var newState = state
        // Dash (CR 702.109a): create delayed trigger to return this permanent to its owner's
        // hand at the beginning of the next end step. Same blink-safety shape as warp above.
        if (spellComponent.wasDashed) {
            val entryTimestamp = newState.getEntity(spellId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent>()
                ?.timestamp
            val (triggerId, allocatedState) = newState.newRoutingId()
            val delayedTrigger = DelayedTriggeredAbility(
                id = triggerId,
                effect = MoveTrackedBattlefieldObjectEffect(
                    target = EffectTarget.SpecificEntity(spellId),
                    destination = Zone.HAND,
                    enteredBattlefieldTimestamp = entryTimestamp
                ),
                fireAtStep = Step.END,
                sourceId = spellId,
                        objectReferences = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(captured = true,
                            origin = newState.objectRef(spellId), source = newState.objectRef(spellId)),
                sourceName = cardComponent?.name ?: "Unknown",
                controllerId = controllerId
            )
            newState = allocatedState.addDelayedTrigger(delayedTrigger)
        }
        return newState
    }

    private fun enterPreparedIfKeyworded(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?,
        controllerId: EntityId
    ): GameState {
        var newState = state
        // Prepared (Secrets of Strixhaven): a preparation creature whose face carries the PREPARED
        // keyword ("This creature enters prepared") enters prepared. Becoming prepared creates a
        // copy of the card's prepare spell in exile that its controller may cast (paying that
        // spell's cost); casting it unprepares the creature. PREPARE-layout creatures that lack the
        // keyword (e.g. Leech Collector, which only becomes prepared via a trigger) do not enter
        // prepared.
        if (cardDef != null && cardDef.layout == com.wingedsheep.sdk.model.CardLayout.PREPARE &&
            cardDef.keywords.contains(com.wingedsheep.sdk.core.Keyword.PREPARED) &&
            !spellComponent.castFaceDown
        ) {
            newState = PreparationLogic.makePrepared(newState, spellId, cardDef, controllerId)
        }
        return newState
    }

    // =========================================================================
    // Enters With Replacements ("enters with counters / keywords", CR 614.1c)
    // =========================================================================

    /**
     * Apply the resolving permanent's "enters with …" replacement effects — counters
     * (EntersWithCounters / EntersWithDynamicCounters) and keywords (EntersWithKeywords) —
     * plus any global ones sourced from other battlefield permanents (e.g., Gev, Scaled
     * Scorch: "Other creatures you control enter with additional +1/+1 counters").
     * Thin wrapper over [EntersWithReplacements] carrying the cast context (X, mana spent).
     */
    internal fun applyEntersWithReplacements(
        state: GameState,
        entityId: EntityId,
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        controllerId: EntityId,
        xValue: Int? = null,
        totalManaSpent: Int = 0
    ): Pair<GameState, List<GameEvent>> {
        var newState = state
        val events = mutableListOf<GameEvent>()

        val (ownState, ownEvents) = EntersWithReplacements.applyFromDefinition(
            newState, entityId, cardDef, controllerId, xValue, totalManaSpent,
            predicateEvaluator = conditionEvaluator.predicates
        )
        newState = ownState
        events.addAll(ownEvents)

        val (globalState, globalEvents) = EntersWithReplacements.applyGlobal(
            newState, entityId, controllerId, cardRegistry,
            predicateEvaluator = conditionEvaluator.predicates
        )
        newState = globalState
        events.addAll(globalEvents)

        return newState to events
    }
}
