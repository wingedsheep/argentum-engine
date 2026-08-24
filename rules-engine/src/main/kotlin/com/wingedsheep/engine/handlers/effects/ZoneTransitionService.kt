package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.CardExiledWithMadnessEvent
import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.GameEvent as EngineGameEvent
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.mechanics.daynight.DayNightService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.combat.*
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.CommanderZoneChoiceAskedComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.handlers.effects.permanent.types.stampDoubleFacedFrontFace
import com.wingedsheep.engine.handlers.effects.permanent.types.withDfcFaceSelfRedirects
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.identity.PutIntoGraveyardThisTurnComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MadnessExiledComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.CardsDiscardedThisTurnComponent
import com.wingedsheep.engine.state.components.player.CardsLeftGraveyardThisTurnComponent
import com.wingedsheep.engine.state.components.player.CardsPutIntoExileThisTurnComponent
import com.wingedsheep.engine.state.components.player.CreatureSubtypesDiedThisTurnComponent
import com.wingedsheep.engine.state.components.player.ArtifactsDiedThisTurnComponent
import com.wingedsheep.engine.state.components.player.CreaturesDiedThisTurnComponent
import com.wingedsheep.engine.state.components.player.NonTokenCreaturesDiedThisTurnComponent
import com.wingedsheep.engine.state.components.player.OpponentCreaturesExiledThisTurnComponent
import com.wingedsheep.engine.state.components.player.PermanentEnteredFaceDownThisTurnComponent
import com.wingedsheep.engine.state.components.player.PermanentLeftBattlefieldThisTurnComponent
import com.wingedsheep.engine.state.components.player.CreatureLeftBattlefieldThisTurnComponent
import com.wingedsheep.engine.state.components.player.PermanentsSacrificedThisTurnComponent
import com.wingedsheep.engine.state.components.player.CreatureCardsPutIntoGraveyardThisTurnComponent
import com.wingedsheep.engine.state.components.player.PlayerDescendedThisTurnComponent
import com.wingedsheep.engine.state.components.player.SacrificedArtifactThisTurnComponent
import com.wingedsheep.engine.state.components.player.SacrificedFoodThisTurnComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersTapped


/**
 * Options controlling how an entity enters a destination zone.
 */
data class ZoneEntryOptions(
    val controllerId: EntityId? = null,
    val libraryPlacement: LibraryPlacement = LibraryPlacement.Top,
    val tapped: Boolean = false,
    val tappedAndAttacking: Boolean = false,
    val faceDown: Boolean = false,
    val morphData: MorphDataComponent? = null,
    /**
     * Which mechanic put this permanent onto the battlefield face down — morph, manifest, cloak
     * or disguise. Stamped onto the permanent as [FaceDownModeComponent]; drives the face-down art
     * every player sees and the ward {2} disguise/cloak list among the face-down characteristics.
     * Null when [faceDown] is false (or for a face-down *exile*, which has no mode marker).
     */
    val faceDownMode: FaceDownMode? = null,
    val skipZoneChangeRedirect: Boolean = false,
    val faceDownExile: Boolean = false,
    val lastKnownAttachedTo: EntityId? = null,
    /**
     * True when this move is the exile of a material chosen to pay a Craft cost (CR 702.167).
     * Stamped onto the emitted [ZoneChangeEvent.craftMaterial] so a SELF "exiled while activating
     * a craft ability" trigger (Market Gnome) can distinguish it from any other exile. Set only by
     * the Craft cost payment, and only for the material exiles — never for the crafted card's own
     * self-exile.
     */
    val craftMaterial: Boolean = false,
    /**
     * The player performing a move into a library — the one who chose the card and watched where
     * it landed, and so the one who may keep seeing it (CR 400.2). Only consulted for a
     * [Zone.LIBRARY] destination at a known position; see
     * [com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils.placementAudience].
     *
     * Left `null` by callers with no player behind the move, which yields no knowledge for anyone.
     */
    val libraryMoverId: EntityId? = null,
    /**
     * True when a move into a library is revealed to every player on the way in, which makes the
     * placement public knowledge rather than the mover's alone. Independent of the source zone —
     * a move out of a public zone is already table-wide without this.
     */
    val libraryMovePublic: Boolean = false
)

/**
 * How to place a card in the library zone.
 */
sealed interface LibraryPlacement {
    data object Top : LibraryPlacement
    data object Bottom : LibraryPlacement
    data object Shuffled : LibraryPlacement
    data class NthFromTop(val position: Int) : LibraryPlacement
}

/**
 * Result of a zone transition.
 */
data class ZoneTransitionResult(
    val state: GameState,
    val events: List<EngineGameEvent>,
    val redirectResult: ZoneChangeRedirectResult? = null,
    val actualDestination: Zone? = null
)

/**
 * Single canonical zone transition pipeline.
 *
 * ALL zone movement in the engine should go through this service.
 * This ensures that every zone change applies the full cleanup/setup pipeline
 * consistently, preventing bugs from missing steps.
 *
 * Pipeline:
 * 1. Look up entity, CardComponent, owner, current zone
 * 2. Capture last-known info if leaving battlefield
 * 3. Check zone change redirect (unless skipZoneChangeRedirect)
 * 4. EXIT CLEANUP if leaving battlefield:
 *    a. cleanupReverseAttachmentLink
 *    b. cleanupCombatReferences
 *    c. stripBattlefieldComponents
 *    d. removeFloatingEffectsTargeting
 * 5. Strip face-down if leaving exile
 * 6. Remove from current zone
 * 7. ENTRY SETUP based on destination
 * 8. Emit ZoneChangeEvent
 * 9. Apply redirect additional effects if any
 */
object ZoneTransitionService {

    /**
     * Handler used to register static abilities and replacement effects on permanents that
     * enter the battlefield through [moveToZone] (reanimation, returns from exile, leyline
     * starts, etc.). Wired by [com.wingedsheep.engine.core.EngineServices] at construction
     * time. The cast pipeline ([com.wingedsheep.engine.mechanics.stack.StackResolver]) places
     * permanents via [GameState.addToZone] directly, not [moveToZone], so it owns its own
     * call to the handler and is unaffected by this wiring.
     */
    lateinit var staticAbilityHandler: StaticAbilityHandler
    lateinit var cardRegistry: CardRegistry

    /** Evaluates the `unless` clause of an entering card's own [EntersTapped]. */
    private val conditionEvaluator = ConditionEvaluator()

    /**
     * Move one entity between zones with full cleanup + setup.
     *
     * @param state The current game state
     * @param entityId The entity to move
     * @param destinationZone The target zone
     * @param options Entry options (tapped, controller override, library placement, etc.)
     * @param fromZoneKey Override the source zone key (if caller already knows it).
     *        If null, the service will find it automatically.
     * @return ZoneTransitionResult with updated state, events, and redirect info
     */
    fun moveToZone(
        state: GameState,
        entityId: EntityId,
        destinationZone: Zone,
        options: ZoneEntryOptions = ZoneEntryOptions(),
        fromZoneKey: ZoneKey? = null
    ): ZoneTransitionResult {
        // 1. Look up entity info
        val container = state.getEntity(entityId)
            ?: return ZoneTransitionResult(state, emptyList())

        val cardComponent = container.get<CardComponent>()
            ?: return ZoneTransitionResult(state, emptyList())

        val ownerId = cardComponent.ownerId
            ?: return ZoneTransitionResult(state, emptyList())

        val currentZoneKey = fromZoneKey ?: findEntityZone(state, entityId)
            ?: return ZoneTransitionResult(state, emptyList())

        val fromZone = currentZoneKey.zoneType
        val leavingBattlefield = fromZone == Zone.BATTLEFIELD

        // 2. Capture last-known info if leaving battlefield (assembled into one EntitySnapshot
        // below). The +1/+1, -1/-1, and total counter counts are derived from this map by the
        // snapshot's accessors, so they are no longer captured as separate scalars.
        var lastKnownCounters: Map<String, Int> = emptyMap()
        var lastKnownPower: Int? = null
        var lastKnownToughness: Int? = null
        var lastKnownTypeLine: TypeLine? = null
        var lastKnownKeywords: Set<String> = emptySet()
        var lastKnownLostAllAbilities = false
        var lastKnownAttachedTo = options.lastKnownAttachedTo
        var lastKnownBlockingOrBlockedByIds: List<EntityId> = emptyList()
        var lastKnownWasAttacking = false
        var lastKnownAttackedDefenderId: EntityId? = null
        var lastKnownWasToken = false
        var lastKnownCreatedBy: EntityId? = null
        var lastKnownDamageDealtByPlayers: Map<EntityId, Int> = emptyMap()
        var lastKnownDamageSources: Set<com.wingedsheep.engine.state.components.battlefield.DamageSourceLki> = emptySet()
        // The {X} this permanent was cast with (DynamicAmount.CastX), captured before the
        // CastChoicesComponent is stripped so dies/leaves triggers reading CastX still see it
        // as last-known information (CR 603.10a).
        var lastKnownCastX: Int? = null
        var lastKnownWasFaceDown = false

        if (leavingBattlefield) {
            val countersComponent = container.get<CountersComponent>()
            lastKnownCounters = countersComponent?.counters
                ?.filterValues { it > 0 }
                ?.mapKeys { (type, _) ->
                    com.wingedsheep.engine.handlers.effects.permanent.counters
                        .counterTypeToString(type)
                }
                ?: emptyMap()
            val projected = state.projectedState
            lastKnownPower = projected.getPower(entityId)
            lastKnownToughness = projected.getToughness(entityId)
            // Capture the projected typeLine so leaves-battlefield triggers see types/subtypes
            // granted by continuous effects (e.g., Ygra makes other creatures Food artifacts).
            lastKnownTypeLine = buildProjectedTypeLine(cardComponent, state, entityId)
            // Capture projected keywords so dies/leaves triggers can check keyword filters
            // (e.g., Jackdaw Savior: "whenever a creature you control with flying dies").
            lastKnownKeywords = projected.getKeywords(entityId)
            // Capture whether the entity had its abilities stripped at leaving time so the
            // dies / leaves-battlefield detectors can suppress the entity's own triggers
            // (e.g., Xu-Ifit reanimating Festering Goblin without its "When this dies" trigger).
            lastKnownLostAllAbilities = projected.hasLostAllAbilities(entityId)
            if (lastKnownAttachedTo == null) {
                lastKnownAttachedTo = container.get<AttachedToComponent>()?.targetId
            }
            // CR 509 combat pairing, captured before the cross-references are torn down: the
            // creatures blocking this one (its BlockedComponent) and the creatures it was blocking
            // (its BlockingComponent). Read by "destroy all creatures blocking or blocked by it".
            run {
                val blockingThis = container.get<BlockedComponent>()?.blockerIds ?: emptyList()
                val blockedByThis = container.get<BlockingComponent>()?.blockedAttackerIds ?: emptyList()
                lastKnownBlockingOrBlockedByIds = (blockingThis + blockedByThis).distinct()
            }
            // Whether it was still an attacking creature as it left (CR 506.4), captured before
            // cleanupCombatReferences strips the live AttackingComponent — "draw a card if it was
            // attacking" (Garna, Bloodfist of Keld) resolves after the death, so it can only read
            // last known information (CR 608.2h).
            lastKnownWasAttacking = container.has<AttackingComponent>()
            // …and *what* it was attacking. CR 802.2a keeps naming a defending player after the
            // creature "is no longer attacking" — the player it *was* attacking before it left
            // combat — so an ability that outlives its own attacking source still has an answer.
            // Mindstab Thrull sacrifices itself before the defending player discards.
            lastKnownAttackedDefenderId = container.get<AttackingComponent>()?.defenderId
            lastKnownWasToken = container.has<TokenComponent>()
            // Which permanent minted this one — a token is gone from state by the time a
            // leaves-the-battlefield trigger gates (CR 704.5d), so "when the token leaves the
            // battlefield" (Dance of Many) can only recognise *its own* token from here.
            lastKnownCreatedBy = container
                .get<com.wingedsheep.engine.state.components.identity.CreatedByComponent>()?.creatorId
            lastKnownDamageDealtByPlayers =
                container.get<DamageDealtByPlayersThisTurnComponent>()?.perPlayer ?: emptyMap()
            lastKnownDamageSources =
                container.get<com.wingedsheep.engine.state.components.battlefield.DamagedBySourcesThisTurnComponent>()
                    ?.sources ?: emptySet()
            lastKnownCastX = container
                .get<com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent>()?.x
            // A card is turned face up as it leaves the battlefield for a graveyard (CR 708.4), and
            // the battlefield entity is gone by trigger-gating time either way, so "whenever a
            // face-down creature you control dies" (Yarus, Roar of the Old Gods) has to read this
            // as last-known information (CR 608.2h).
            lastKnownWasFaceDown = container
                .has<com.wingedsheep.engine.state.components.identity.FaceDownComponent>()
        }

        // 3. Check zone change redirect (unless skipped)
        val redirectResult = if (!options.skipZoneChangeRedirect) {
            ZoneMovementUtils.checkZoneChangeRedirect(state, entityId, fromZone, destinationZone)
        } else {
            ZoneChangeRedirectResult(destinationZone)
        }
        val actualDestZone = redirectResult.destinationZone

        // A card-intrinsic redirect into the library shuffles the card in rather than placing it on
        // top (Darksteel Colossus, Progenitus). This holds even when the caller skipped the redirect
        // check and passed the result in via the destination zone — such callers set libraryPlacement
        // themselves, so honour whichever is Shuffled.
        val effectiveLibraryPlacement =
            if (redirectResult.shuffleIntoLibrary && actualDestZone == Zone.LIBRARY) {
                LibraryPlacement.Shuffled
            } else {
                options.libraryPlacement
            }

        // Determine controller and destination zone key. Control-changing effects (Threaten,
        // Empress Galina) live in Layer 2 of the projection and never touch the base
        // ControllerComponent, so a battlefield exit must read the projected controller first —
        // it becomes the last-known controller (CR 608.2h) carried on the snapshot and credited
        // by the per-player LTB/death trackers below.
        val controllerId = if (leavingBattlefield) {
            state.projectedState.getController(entityId)
                ?: container.get<ControllerComponent>()?.playerId
                ?: ownerId
        } else {
            ownerId
        }

        val destControllerId = options.controllerId ?: ownerId
        val destZoneKey = if (actualDestZone == Zone.BATTLEFIELD) {
            ZoneKey(destControllerId, actualDestZone)
        } else {
            ZoneKey(ownerId, actualDestZone)
        }

        // One frozen snapshot of the permanent as it last existed on the battlefield
        // (CR 113.7a / 603.10 / 608.2h), or null when this transition doesn't leave the
        // battlefield. Carried on the ZoneChangeEvent for trigger resolution AND stashed on the
        // entity itself (LastKnownPermanentComponent) for resolution-time reads that outlive the
        // permanent ("Destroy target creature. Its controller creates two Map tokens.").
        // Was this permanent equipped / enchanted as it left? The live attachment links are torn
        // down by the exit cleanup below (CR 704.5m/n), so "modified/equipped/enchanted creature
        // leaves the battlefield" triggers must freeze it here as last-known information (CR 608.2h).
        val lastKnownAttachmentIds = if (leavingBattlefield) {
            state.getEntity(entityId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent>()
                ?.attachedIds
                ?: emptyList()
        } else emptyList()
        val lastKnownAttachedTypeLines = lastKnownAttachmentIds.mapNotNull { attachId ->
            state.getEntity(attachId)
                ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.typeLine
        }
        val lastKnownWasEquipped = lastKnownAttachedTypeLines.any { it.isEquipment }
        val lastKnownWasEnchanted = lastKnownAttachedTypeLines.any { it.isAura }

        val lastKnownSnapshot = if (leavingBattlefield) {
            com.wingedsheep.engine.state.components.stack.EntitySnapshot(
                entityId = entityId,
                power = lastKnownPower,
                toughness = lastKnownToughness,
                // Mirror the projected type line's subtypes into the snapshot's own `subtypes`
                // field so it carries the same meaning here as on the `fromProjection` path.
                // Read by `TriggeringEntityHadSubtype` ("if it wasn't a Demon" — Infernal Vessel),
                // which must see continuous-effect-granted types, not just printed ones.
                subtypes = lastKnownTypeLine?.subtypes?.mapTo(mutableSetOf()) { it.value } ?: emptySet(),
                controllerId = controllerId,
                counters = lastKnownCounters,
                keywords = lastKnownKeywords,
                lostAllAbilities = lastKnownLostAllAbilities,
                typeLine = lastKnownTypeLine,
                cardDefinitionId = cardComponent.cardDefinitionId,
                attachedTo = lastKnownAttachedTo,
                wasEquipped = lastKnownWasEquipped,
                attachmentIds = lastKnownAttachmentIds,
                wasEnchanted = lastKnownWasEnchanted,
                blockingOrBlockedByIds = lastKnownBlockingOrBlockedByIds,
                wasAttacking = lastKnownWasAttacking,
                attackedDefenderId = lastKnownAttackedDefenderId,
                wasToken = lastKnownWasToken,
                createdBy = lastKnownCreatedBy,
                damageDealtByPlayers = lastKnownDamageDealtByPlayers,
                damageSources = lastKnownDamageSources,
                wasFaceDown = lastKnownWasFaceDown,
            )
        } else null

        var newState = state
        val events = mutableListOf<EngineGameEvent>()

        // 4. EXIT CLEANUP if leaving battlefield
        if (leavingBattlefield) {
            // An attached Aura/Equipment that leaves the battlefield becomes unattached from its
            // host (CR 701.3d) — Stitcher's Graft's "sacrifice that permanent" fires off exactly
            // this when the Equipment is destroyed. Reported here, before the link and the
            // ControllerComponent are cleared, so the trigger has its last-known information
            // (CR 603.6e). The three unattaches that leave the attachment on the battlefield run
            // through ZoneMovementUtils.unattachEmittingEvent instead.
            newState.getEntity(entityId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
                ?.let { attached ->
                    events += com.wingedsheep.engine.core.PermanentUnattachedEvent(
                        attachmentId = entityId,
                        attachmentName = cardComponent.name,
                        attachedToId = attached.targetId,
                        // `controllerId` above is already the projected last-known controller.
                        controllerId = controllerId,
                    )
                }
            newState = cleanupReverseAttachmentLink(newState, entityId)
            newState = cleanupCombatReferences(newState, entityId)
            // Equipment/Auras attached *to* this permanent come off when their host leaves the
            // battlefield (CR 704.5n unattaches Equipment, 704.5m graveyards Auras) — applied by
            // the UnattachedAurasCheck state-based action,
            // which runs after any "when equipped creature dies/leaves" triggers (Forebears Blade)
            // have been detected, so the attachment must NOT be cleared eagerly here. We only *mark*
            // the attachments now: the host's EntityId is reused across a blink (exile→battlefield),
            // so the SBA can't otherwise tell the host left and returned as a new object.
            newState = ZoneMovementUtils.markAttachmentsHostLeft(newState, entityId)

            // Freeze last-known information onto any pending "copy your next spell" rider this
            // permanent created (CR 608.2h / 113.7a). The rider outlives its source — the ability
            // already resolved, so killing the source doesn't remove it — but its `spellFilter` may
            // read the source's own characteristics, e.g. Loki Laufeyson's "mana value less than or
            // equal to Loki's power". Once the source is gone the filter must use the characteristics
            // it last had on the battlefield, not its printed ones.
            //
            // Stamped here, at departure, rather than when the rider was created: the source's power
            // can change in between (Loki arms the rider at 2/1, powers up to 4/3, then dies — the
            // cap is 4, not 2). Only the first departure stamps; a rider whose source already left
            // keeps that snapshot, so a later blink of the same EntityId can't overwrite the
            // last-known state the rider is entitled to read.
            if (lastKnownSnapshot != null) {
                val pending = newState.pendingSpellCopies
                if (pending.any { it.sourceId == entityId && it.lastKnownSourceSnapshot == null }) {
                    newState = newState.copy(
                        pendingSpellCopies = pending.map { copy ->
                            if (copy.sourceId == entityId && copy.lastKnownSourceSnapshot == null) {
                                copy.copy(lastKnownSourceSnapshot = lastKnownSnapshot)
                            } else copy
                        }
                    )
                }
            }
        }

        // 5. Strip face-down if leaving exile
        if (fromZone == Zone.EXILE) {
            val entityContainer = newState.getEntity(entityId)
            if (entityContainer != null && entityContainer.has<FaceDownComponent>()) {
                newState = newState.updateEntity(entityId) { c -> c.without<FaceDownComponent>() }
            }
            // The "which zone was this exiled from" stamp is only meaningful while the object is
            // actually in exile, so drop it on the way out. Two exits reuse the entity id and
            // don't come through here — StackResolver's cast-from-exile and
            // ReturnOneFromLinkedExileExecutor — and each clears it itself. An exile → exile move
            // (CR 406.7) re-stamps it below.
            if (entityContainer != null &&
                entityContainer.has<com.wingedsheep.engine.state.components.identity.ExiledFromZoneComponent>()
            ) {
                newState = newState.updateEntity(entityId) { c ->
                    c.without<com.wingedsheep.engine.state.components.identity.ExiledFromZoneComponent>()
                }
            }
            // A suspended card that leaves exile by any non-cast path (returned to hand,
            // shuffled in, exiled elsewhere) is no longer suspended (CR 702.62). The cast
            // path is guarded separately by the countdown's intervening-if, so a leftover
            // marker is inert there.
            if (entityContainer != null &&
                entityContainer.has<com.wingedsheep.engine.state.components.battlefield.SuspendedComponent>()
            ) {
                newState = newState.updateEntity(entityId) { c ->
                    c.without<com.wingedsheep.engine.state.components.battlefield.SuspendedComponent>()
                }
            }
            // Likewise a Paradigm card that leaves exile by any non-cast path stops recurring —
            // the marker is what the engine keys on, so drop it.
            if (entityContainer != null &&
                entityContainer.has<com.wingedsheep.engine.state.components.battlefield.ParadigmComponent>()
            ) {
                newState = newState.updateEntity(entityId) { c ->
                    c.without<com.wingedsheep.engine.state.components.battlefield.ParadigmComponent>()
                }
            }
            // A madness card leaving exile — cast, put into the graveyard by its own trigger, or
            // moved by anything else — is done with madness (CR 702.35a offers the cast once). Drop
            // the marker *and* the fixed madness cost it published: a lingering fixed alternative
            // cost would silently re-price a later flashback-style cast from the graveyard.
            if (entityContainer != null && entityContainer.has<MadnessExiledComponent>()) {
                newState = newState.updateEntity(entityId) { c ->
                    c.without<MadnessExiledComponent>()
                        .without<PlayWithFixedAlternativeManaCostComponent>()
                }
            }
        }

        // 6. Remove from current zone
        // Use the provided fromZoneKey directly — it already identifies the correct zone.
        // Don't derive from ControllerComponent, as the card may be on a different
        // player's battlefield zone (e.g., control-changed permanents in some zone layouts).
        val removeZoneKey = currentZoneKey
        newState = newState.removeFromZone(removeZoneKey, entityId)

        // Drop any remaining linked-exile reference held by a granter still on the
        // battlefield (e.g. Maralen, Fae Ascendant). The card has just left exile by
        // some non-cast path — return, blink, exile-elsewhere — so the granter must
        // forget it. Cast paths through StackResolver.removeFromCurrentZone unlink
        // separately because they bypass this service.
        if (fromZone == Zone.EXILE) {
            newState = ZoneMovementUtils.unlinkFromAllLinkedExiles(newState, entityId)
        }

        // A static ability granted to a card *while it sat in the graveyard* — Case of the Uneaten
        // Feast's "creature cards in your graveyard gain 'You may cast this card from your
        // graveyard'" — ends when the card leaves that zone (CR 400.7). The battlefield-exit prune
        // below only covers grants held by permanents, and the cast path never reaches this service
        // at all (StackResolver.removeFromCurrentZone prunes there), so this is what catches a
        // graveyard card returned to hand, exiled, or shuffled away and then put back the same turn.
        if (fromZone == Zone.GRAVEYARD && actualDestZone != Zone.GRAVEYARD) {
            newState = newState.copy(
                grantedStaticAbilities = newState.grantedStaticAbilities.filter { it.entityId != entityId }
            )
        }

        // Strip battlefield components and remove floating effects AFTER removal
        if (leavingBattlefield) {
            // Capture LinkedExileComponent BEFORE stripping so LTB triggers (e.g. Seam Rip's
            // "return linked exile" on LeavesBattlefield) can still read it.
            // Rule 400.7 only applies once the card re-enters the battlefield as a new object;
            // until then, graveyard/exile instances need the component for last-known-info triggers.
            val preStripLinkedExile = newState.getEntity(entityId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent>()
            // Same last-known-info preservation for the noted-exile snapshot (Tawnos's Coffin):
            // its "return the exiled card with the noted counters" LTB trigger reads it after the
            // source has left.
            val preStripNotedExile = newState.getEntity(entityId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.NotedExileComponent>()

            // Revert permanent-level copy effects (Clone / Mockingbird / "becomes a copy of").
            // Per CR 400.7, a card that changes zones becomes a new object — its copy effect
            // ends and the card returns to its printed characteristics.
            val copyOf = newState.getEntity(entityId)
                ?.get<com.wingedsheep.engine.state.components.identity.CopyOfComponent>()
            val originalCardComponent = copyOf?.originalCardComponent
            if (originalCardComponent != null) {
                newState = newState.updateEntity(entityId) { c -> c.with(originalCardComponent) }
            }

            newState = newState.updateEntity(entityId) { c -> stripBattlefieldComponents(c) }
            newState = removeFloatingEffectsTargeting(newState, entityId)

            // A permanent's battlefield-scoped granted *static* abilities end when it leaves the
            // battlefield (CR 400.7 — the card becomes a new object with no memory). These live in
            // their own GameState list with no floating-effect representation, so prune them here
            // alongside the floating effects. Without this a grant like Roar of the Fifth People's
            // chapter II ("creatures you control have '{T}: Add {R}, {G}, or {W}'") lingers on the
            // card after it hits the graveyard — inert in play (the ability enumerators gate on the
            // granter still being on the battlefield) but still surfaced as an active-effect badge.
            // Player-anchored grants (entityId = a player, e.g. Malicious Eclipse) are untouched.
            newState = newState.copy(
                grantedStaticAbilities = newState.grantedStaticAbilities.filter { it.entityId != entityId }
            )

            // Re-attach LinkedExileComponent on any non-battlefield destination so LTB triggers
            // that reference it (like Seam Rip's return effect or Champion of the Clachan's
            // bounce-back) still have access to it after the source has left. Rule 400.7 is
            // honoured because applyBattlefieldEntry strips the component when the card
            // re-enters the battlefield as a new object.
            if (preStripLinkedExile != null && actualDestZone != Zone.BATTLEFIELD) {
                newState = newState.updateEntity(entityId) { c ->
                    c.with(preStripLinkedExile)
                }
            }
            if (preStripNotedExile != null && actualDestZone != Zone.BATTLEFIELD) {
                newState = newState.updateEntity(entityId) { c ->
                    c.with(preStripNotedExile)
                }
            }
            // Stash the battlefield-exit snapshot on the entity so resolution-time reads that
            // outlive the permanent use last-known information (CR 608.2h) — e.g. TargetController
            // after an earlier step of the same effect destroyed the target. Skipped when a
            // redirect keeps the object on the battlefield (it stays live there).
            if (lastKnownSnapshot != null && actualDestZone != Zone.BATTLEFIELD) {
                newState = newState.updateEntity(entityId) { c ->
                    c.with(LastKnownPermanentComponent(lastKnownSnapshot))
                }
            }
        } else {
            // Any further zone change makes a new object (CR 400.7): information about the old
            // battlefield incarnation must not survive it. No-op when the component is absent.
            newState = newState.updateEntity(entityId) { c ->
                c.without<LastKnownPermanentComponent>()
            }
        }

        // 7. ENTRY SETUP based on destination
        when (actualDestZone) {
            Zone.BATTLEFIELD -> {
                // Rule 400.7: a card that changes zones becomes a new object with no memory
                // of its previous existence. Floating effects targeting it are stripped on the
                // way out (see step 4); granted triggered/activated abilities have to be
                // dropped here, on re-entry, because the leaves-battlefield trigger detection
                // for the previous incarnation needs to read them via state during the exit
                // event. By the time we reach this point those triggers are already queued on
                // the stack with their own captured ability data, so it is safe to wipe.
                newState = newState.copy(
                    grantedTriggeredAbilities = newState.grantedTriggeredAbilities
                        .filter { it.entityId != entityId },
                    grantedActivatedAbilities = newState.grantedActivatedAbilities
                        .filter { it.entityId != entityId }
                )
                newState = newState.addToZone(destZoneKey, entityId)
                newState = applyBattlefieldEntry(
                    newState, entityId, cardComponent, destControllerId, options, fromZone
                )
                // Record entry for per-player ETB-by-type tracking (Mechan Shieldmate and similar).
                // This pipeline records via PermanentEntryTracker.record directly rather than
                // BattlefieldEntry.place because the read must happen *after* applyBattlefieldEntry
                // wires the controller — only then does projection see the right controller.
                newState = PermanentEntryTracker.record(newState, destControllerId, entityId)
                // Handle Saga entering the battlefield (Rule 714.3a)
                val (sagaState, sagaEvents) = applySagaEntryIfNeeded(newState, entityId)
                newState = sagaState
                events.addAll(sagaEvents)
                // Handle the intrinsic entry counters of a planeswalker (loyalty, CR 306.5b) or a
                // battle (defense, CR 310.4b). The helper skips face-down entries itself — a
                // face-down permanent is a nameless 2/2 creature with no printed loyalty or
                // defense (CR 708.2a).
                if (::cardRegistry.isInitialized) {
                    val (entryCounterState, entryCounterEvents) = applyIntrinsicEntryCountersIfNeeded(
                        newState, entityId, destControllerId, cardRegistry
                    )
                    newState = entryCounterState
                    events.addAll(entryCounterEvents)
                }
            }
            Zone.LIBRARY -> {
                if (effectiveLibraryPlacement is LibraryPlacement.Shuffled) {
                    // Drop reveals on every other library card before mixing the new one in
                    newState = com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils
                        .clearLibraryReveals(newState, ownerId)
                }
                newState = placeInLibrary(newState, entityId, destZoneKey, effectiveLibraryPlacement)
                if (effectiveLibraryPlacement is LibraryPlacement.Shuffled) {
                    events.add(com.wingedsheep.engine.core.LibraryShuffledEvent(ownerId))
                }
                // Every library entry in the engine passes through here, which makes this the one
                // place that decides who is allowed to keep seeing the card. It *replaces* the
                // card's reveal audience rather than adding to it, so whatever the card was known
                // as elsewhere — revealed in a hand, public on the battlefield — does not survive
                // being tucked away unless this placement itself grants it. A caller that says
                // nothing gets the safe answer: nobody knows.
                newState = com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils
                    .setPlacementKnowledge(
                        newState,
                        listOf(entityId),
                        com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils
                            .placementAudience(
                                fromZone = fromZone,
                                publiclyRevealed = options.libraryMovePublic,
                                moverId = options.libraryMoverId,
                                allPlayers = newState.turnOrder,
                                knownPosition = effectiveLibraryPlacement !is LibraryPlacement.Shuffled,
                            )
                    )
            }
            Zone.EXILE -> {
                newState = newState.addToZone(destZoneKey, entityId)
                // Record where this object came from, so a CR 610.3 "return it to its previous
                // zone" effect can put it back (`CardDestination.ToZoneExiledFrom`). Stamped for
                // every exile that comes through here, not just linked ones. This is the main
                // road, not a choke point: the direct-`addToZone` exiles listed on
                // ExiledFromZoneComponent write the same stamp themselves, and anything still
                // unstamped takes ToZoneExiledFrom's fallback.
                newState = newState.updateEntity(entityId) { c ->
                    c.with(
                        com.wingedsheep.engine.state.components.identity
                            .ExiledFromZoneComponent(fromZone)
                    )
                }
                if (options.faceDownExile) {
                    newState = newState.updateEntity(entityId) { c -> c.with(FaceDownComponent) }
                }
                // Link the exiled card to a RedirectZoneChange(linkToSource) source (Valgavoth).
                redirectResult.linkSourceId?.let { sourceId ->
                    newState = ZoneMovementUtils.linkExiledToSource(newState, entityId, sourceId)
                }
            }
            else -> {
                // HAND, GRAVEYARD, STACK — simple addToZone
                newState = newState.addToZone(destZoneKey, entityId)
            }
        }

        // 7b. Rule 712.8a: while a DFC is in a zone other than the battlefield or stack, it has
        // only the characteristics of its front face. Restore the saved front-face CardComponent.
        if (actualDestZone != Zone.BATTLEFIELD && actualDestZone != Zone.STACK) {
            val entityContainer = newState.getEntity(entityId)
            if (entityContainer != null) {
                val dfc = entityContainer.get<DoubleFacedComponent>()
                if (dfc != null && dfc.isBack && dfc.frontFaceCard != null) {
                    // The front face's own "from anywhere" self-replacements come back with it —
                    // and, just as importantly, the back face's stop applying. A disturbed creature
                    // that is exiled by its own back-face clause reverts to a plain front face.
                    val frontDef = if (::cardRegistry.isInitialized) {
                        cardRegistry.getCard(dfc.frontCardDefinitionId)
                    } else null
                    newState = newState.updateEntity(entityId) { c ->
                        val reverted = c.with(dfc.frontFaceCard)
                            .with(dfc.copy(currentFace = DoubleFacedComponent.Face.FRONT, frontFaceCard = null))
                        if (frontDef != null) withDfcFaceSelfRedirects(reverted, frontDef) else reverted
                    }
                }
            }
        }

        // 7c. Clear the CR 903.9a "already asked this stay" marker on every commander zone
        // change. The marker is attached by CommanderZoneChoiceCheck when the owner declines
        // the prompt; clearing it here means the next entry into a non-command zone produces a
        // fresh question, matching the rule's "since the last time state-based actions were
        // checked" wording (a new zone entry resets the clock). Cheap unconditional strip —
        // without<> is a no-op when the component is absent.
        if (newState.getEntity(entityId)?.has<CommanderComponent>() == true) {
            newState = newState.updateEntity(entityId) { c ->
                c.without<CommanderZoneChoiceAskedComponent>()
            }
        }

        // 8. Emit ZoneChangeEvent. A battlefield exit is a sacrifice (CR 701.21) when the central
        // sacrifice hook (trackPermanentSacrifice) pre-marked this entity in pendingSacrificeIds —
        // every sacrifice site routes through that hook, so the dozen call sites stay flag-free.
        // Consume the marker so a later non-sacrifice move of the same id isn't mis-tagged.
        val wasSacrificed = leavingBattlefield && entityId in newState.pendingSacrificeIds
        if (entityId in newState.pendingSacrificeIds) {
            newState = newState.copy(pendingSacrificeIds = newState.pendingSacrificeIds - entityId)
        }
        // Same one-shot lifetime for the discard-cause marker: the redirect check above has already
        // read it, so drop it before any later move of the same card can see a stale cause.
        if (entityId in newState.pendingDiscardCauseControllers) {
            newState = newState.copy(
                pendingDiscardCauseControllers = newState.pendingDiscardCauseControllers - entityId
            )
        }
        events.add(
            ZoneChangeEvent(
                entityId = entityId,
                entityName = cardComponent.name,
                fromZone = fromZone,
                toZone = actualDestZone,
                ownerId = ownerId,
                // The former ~16 lastKnown* scalars are now the snapshot's fields; the counter
                // counts derive from its `counters` map (plusOnePlusOneCounters / etc.).
                lastKnown = lastKnownSnapshot,
                xValue = lastKnownCastX,
                wasSacrificed = wasSacrificed,
                // Only a battlefield exit can be a craft-material exile; the flag is carried on the
                // ZoneEntryOptions by the Craft cost payment for each chosen material.
                craftMaterial = leavingBattlefield && options.craftMaterial
            )
        )

        // 8a2. Void: track that a nonland permanent left the battlefield this turn.
        // Uses the last-known projected type line so that creature-lands (which carry the
        // land type) correctly do NOT enable void.
        if (leavingBattlefield && lastKnownTypeLine?.isLand == false) {
            newState = newState.copy(nonlandPermanentLeftBattlefieldThisTurn = true)
        }

        // 8a3. Per-player permanent-LTB tracking (Shortcut to Mushrooms, LTR).
        // Counts every permanent type — including lands and tokens — leaving the
        // battlefield this turn under the last-known controller. Credited to the
        // last-known controller (which defaults to ownerId when no ControllerComponent
        // is present) so a Threaten-style steal-and-sacrifice counts for the thief.
        if (leavingBattlefield) {
            newState = newState.updateEntity(controllerId) { playerContainer ->
                val existing = playerContainer.get<PermanentLeftBattlefieldThisTurnComponent>()
                    ?: PermanentLeftBattlefieldThisTurnComponent()
                playerContainer.with(PermanentLeftBattlefieldThisTurnComponent(existing.count + 1))
            }
            // Creature-scoped sibling (Kutzil's Flanker). Uses the last-known projected type line so
            // a creature-land counts only if it was a creature as it left.
            if (lastKnownTypeLine?.isCreature == true) {
                newState = newState.updateEntity(controllerId) { playerContainer ->
                    val existing = playerContainer.get<CreatureLeftBattlefieldThisTurnComponent>()
                        ?: CreatureLeftBattlefieldThisTurnComponent()
                    playerContainer.with(CreatureLeftBattlefieldThisTurnComponent(existing.count + 1))
                }
            }
        }

        // 8b. Track creature deaths inline so subsequent effects can see counts
        if (leavingBattlefield && actualDestZone == Zone.GRAVEYARD && cardComponent.typeLine.isCreature) {
            val isToken = container.has<TokenComponent>()
            // Track all creature deaths (including tokens)
            newState = newState.updateEntity(controllerId) { playerContainer ->
                val existing = playerContainer.get<CreaturesDiedThisTurnComponent>()
                    ?: CreaturesDiedThisTurnComponent()
                playerContainer.with(CreaturesDiedThisTurnComponent(existing.count + 1))
            }
            // Track non-token creature deaths separately
            if (!isToken) {
                newState = newState.updateEntity(controllerId) { playerContainer ->
                    val existing = playerContainer.get<NonTokenCreaturesDiedThisTurnComponent>()
                        ?: NonTokenCreaturesDiedThisTurnComponent()
                    playerContainer.with(NonTokenCreaturesDiedThisTurnComponent(existing.count + 1))
                }
            }
            // Record the dying creature's last-known subtypes (CR 603.10 / last-known information)
            // so subtype-filtered death conditions can match it (e.g. "a non-Zombie creature died
            // this turn"). Falls back to the base type line if no projected snapshot was captured.
            val diedSubtypes = (lastKnownTypeLine ?: cardComponent.typeLine)
                .subtypes.map { it.value }.toSet()
            newState = newState.updateEntity(controllerId) { playerContainer ->
                val existing = playerContainer.get<CreatureSubtypesDiedThisTurnComponent>()
                    ?: CreatureSubtypesDiedThisTurnComponent()
                playerContainer.with(
                    CreatureSubtypesDiedThisTurnComponent(existing.diedSubtypeSets + listOf(diedSubtypes))
                )
            }
        }

        // 8b1. Track artifacts put into a graveyard from the battlefield (Anzrag's Rampage).
        // The artifact-typed sibling of 8b, credited to the same last-known controller. Reads the
        // last-known *projected* type line (like 8a3, unlike 8b's base read) so an artifact that
        // was only an artifact through a continuous effect still counts, and an animated artifact
        // creature counts as both. Summed over Player.Each this is the game-wide "artifacts that
        // were put into graveyards from the battlefield this turn".
        if (leavingBattlefield && actualDestZone == Zone.GRAVEYARD &&
            (lastKnownTypeLine ?: cardComponent.typeLine).isArtifact
        ) {
            newState = newState.updateEntity(controllerId) { playerContainer ->
                val existing = playerContainer.get<ArtifactsDiedThisTurnComponent>()
                    ?: ArtifactsDiedThisTurnComponent()
                playerContainer.with(ArtifactsDiedThisTurnComponent(existing.count + 1))
            }
        }

        // 8b2. Track creatures exiled from battlefield for opponent's tracking
        // Used by Vren, the Relentless: "creatures exiled under your opponents' control this turn"
        if (leavingBattlefield && actualDestZone == Zone.EXILE && cardComponent.typeLine.isCreature) {
            // For each opponent of the creature's controller, increment their exile count
            val allPlayers = newState.turnOrder
            for (opponentId in allPlayers) {
                if (opponentId != controllerId) {
                    newState = newState.updateEntity(opponentId) { playerContainer ->
                        val existing = playerContainer.get<OpponentCreaturesExiledThisTurnComponent>()
                            ?: OpponentCreaturesExiledThisTurnComponent()
                        playerContainer.with(OpponentCreaturesExiledThisTurnComponent(existing.count + 1))
                    }
                }
            }
        }

        // 8b2b. Track cards put into exile this turn, keyed on the card's owner. Summed across
        // all players this gives the game-wide "cards put into exile this turn" count (Ennis,
        // Debate Moderator). Tokens are excluded — a token briefly placed in exile isn't a card —
        // and exile→exile shuffles don't count as a card being "put into exile".
        if (actualDestZone == Zone.EXILE && fromZone != Zone.EXILE && !container.has<TokenComponent>()) {
            newState = newState.updateEntity(ownerId) { playerContainer ->
                val existing = playerContainer.get<CardsPutIntoExileThisTurnComponent>()
                    ?: CardsPutIntoExileThisTurnComponent()
                playerContainer.with(CardsPutIntoExileThisTurnComponent(existing.count + 1))
            }
        }

        // 8b3. Stamp "put into a graveyard this turn" on the card entity, recording whether
        // the arrival came from the battlefield. Backs Abyssal Harvester (FDN, any origin
        // zone) and Samwise the Stouthearted / Lobelia Sackville-Baggins (LTR, battlefield
        // only). Overwrites any previous stamp so a card that bounces into a graveyard twice
        // in one turn records its most recent origin.
        if (actualDestZone == Zone.GRAVEYARD && fromZone != Zone.GRAVEYARD) {
            newState = newState.updateEntity(entityId) { c ->
                c.with(PutIntoGraveyardThisTurnComponent(fromBattlefield = leavingBattlefield))
            }
        }

        // 8c. Track cards leaving the graveyard
        if (fromZone == Zone.GRAVEYARD) {
            // Strip the "put into a graveyard this turn" stamp — the card is no longer in a
            // graveyard, so neither predicate may carry over to a later graveyard arrival
            // via a different path.
            newState = newState.updateEntity(entityId) { c ->
                c.without<PutIntoGraveyardThisTurnComponent>()
            }
            // Likewise drop the Mayhem "you discarded this card this turn" gate mark: once the card
            // leaves the graveyard it becomes a new object on any later return (CR 400.7), so it is
            // no longer the card you discarded. (Casting via Mayhem bypasses moveToZone and prunes
            // in CastSpellHandler instead; this covers reanimation / exile / bounce out of the yard.)
            newState = untrackDiscardedCard(newState, entityId)
            newState = newState.updateEntity(ownerId) { playerContainer ->
                val existing = playerContainer.get<CardsLeftGraveyardThisTurnComponent>()
                    ?: CardsLeftGraveyardThisTurnComponent()
                playerContainer.with(CardsLeftGraveyardThisTurnComponent(existing.count + 1))
            }
        }

        // 8d. Descend (CR 700.11): track permanent cards put into a player's graveyard
        // from any zone. Tokens are excluded per Scryfall ruling — although tokens are
        // briefly placed in the graveyard before ceasing to exist, that placement does
        // not count as the owner having descended. Non-permanent cards (instants /
        // sorceries entering the graveyard from the stack, hand, or library) are also
        // excluded. The count is keyed on the card's owner, not its last controller —
        // "your graveyard" is the owner's graveyard.
        if (actualDestZone == Zone.GRAVEYARD &&
            cardComponent.typeLine.isPermanent &&
            !container.has<TokenComponent>()
        ) {
            newState = newState.updateEntity(ownerId) { playerContainer ->
                val existing = playerContainer.get<PlayerDescendedThisTurnComponent>()
                    ?: PlayerDescendedThisTurnComponent()
                playerContainer.with(PlayerDescendedThisTurnComponent(existing.count + 1))
            }
        }

        // 8d2. "A creature card was put into your graveyard from anywhere this turn" (Macabre
        // Reconstruction). The creature-typed slice of the same arrival: any origin zone, keyed
        // on the owner, tokens excluded. Turn history — reanimating the card later in the turn
        // doesn't undo the count.
        if (actualDestZone == Zone.GRAVEYARD &&
            fromZone != Zone.GRAVEYARD &&
            cardComponent.typeLine.isCreature &&
            !container.has<TokenComponent>()
        ) {
            newState = newState.updateEntity(ownerId) { playerContainer ->
                val existing = playerContainer.get<CreatureCardsPutIntoGraveyardThisTurnComponent>()
                    ?: CreatureCardsPutIntoGraveyardThisTurnComponent()
                playerContainer.with(CreatureCardsPutIntoGraveyardThisTurnComponent(existing.count + 1))
            }
        }

        // 8e. Madness (CR 702.35a) — this move was a discard that the madness replacement diverted
        // into exile. Mark the card so only a *discarded-into-exile* card gets the CR 702.35a cast
        // offer, and publish the madness cost as a fixed alternative mana cost so the ordinary
        // cast-from-exile machinery charges it instead of the printed cost (CR 702.35b). The event
        // is what the trigger detector turns into the cast offer; it is emitted after the
        // ZoneChangeEvent so the exile is already history by the time the trigger is built.
        if (!options.skipZoneChangeRedirect && actualDestZone == Zone.EXILE) {
            val madnessCost = ZoneMovementUtils.madnessDiscardExile(
                state, entityId, container, fromZone, destinationZone
            )
            if (madnessCost != null) {
                newState = newState.updateEntity(entityId) { c ->
                    c.with(MadnessExiledComponent(ownerId))
                        .with(PlayWithFixedAlternativeManaCostComponent(ownerId, madnessCost))
                }
                events.add(CardExiledWithMadnessEvent(ownerId, entityId, cardComponent.name))
            }
        }

        // 9. Apply redirect additional effects if any
        if (redirectResult.additionalEffect != null) {
            val (updatedState, extraEvents) = ZoneMovementUtils.applyReplacementAdditionalEffect(
                newState, redirectResult.additionalEffect, redirectResult.effectControllerId, entityId,
                sourceId = redirectResult.effectSourceId
            )
            newState = updatedState
            events.addAll(extraEvents)
        }

        return ZoneTransitionResult(
            state = newState,
            events = events,
            redirectResult = redirectResult,
            actualDestination = actualDestZone
        )
    }

    /**
     * Move multiple entities to a zone. For library destinations, defers
     * shuffling until all cards are placed.
     */
    fun moveToZoneBatch(
        state: GameState,
        entityIds: List<EntityId>,
        destinationZone: Zone,
        options: ZoneEntryOptions = ZoneEntryOptions()
    ): ZoneTransitionResult {
        var currentState = state
        val allEvents = mutableListOf<EngineGameEvent>()

        for (entityId in entityIds) {
            // For batch library moves with Shuffled placement, don't shuffle per-card
            val perCardOptions = if (destinationZone == Zone.LIBRARY &&
                options.libraryPlacement is LibraryPlacement.Shuffled
            ) {
                // Place at bottom first, shuffle once at end
                options.copy(libraryPlacement = LibraryPlacement.Bottom)
            } else {
                options
            }

            val result = moveToZone(currentState, entityId, destinationZone, perCardOptions)
            currentState = result.state
            allEvents.addAll(result.events)
        }

        // Final shuffle if needed
        if (destinationZone == Zone.LIBRARY && options.libraryPlacement is LibraryPlacement.Shuffled) {
            // Find the owner from the first entity (all should have same owner for batch)
            val ownerId = entityIds.firstOrNull()?.let {
                state.getEntity(it)?.get<CardComponent>()?.ownerId
            }
            if (ownerId != null) {
                currentState = com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils
                    .clearLibraryReveals(currentState, ownerId)
                val libraryZone = ZoneKey(ownerId, Zone.LIBRARY)
                val (library, shuffledState) = currentState.nextRandom { shuffle(currentState.getZone(libraryZone)) }
                currentState = shuffledState.copy(zones = shuffledState.zones + (libraryZone to library))
                allEvents.add(com.wingedsheep.engine.core.LibraryShuffledEvent(ownerId))
            }
        }

        return ZoneTransitionResult(state = currentState, events = allEvents)
    }

    /**
     * Move a card from a player's hand to their graveyard as a discard.
     *
     * Emits the standard `CardsDiscardedEvent` plus the `ZoneChangeEvent` produced by
     * `moveToZone`, so dies/discard triggers and animations both see the canonical pair.
     */
    fun discardCard(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId,
        causedByControllerId: EntityId? = null
    ): ZoneTransitionResult =
        discardCards(state, playerId, listOf(cardId), causedByControllerId)

    /**
     * Central "these cards are being discarded because of a spell or ability" hook, mirroring
     * [trackPermanentSacrifice]. Records the causing object's controller in
     * [GameState.pendingDiscardCauseControllers] so the imminent `moveToZone` can offer the cause to
     * zone-change replacements (Wilt-Leaf Liege) without every discard site threading an explicit
     * parameter through the move.
     *
     * Pass null — or skip the call — for discards with no spell/ability cause: the CR 514.1
     * hand-size discard, and discards made to pay a cost.
     */
    fun markDiscardCause(
        state: GameState,
        cardIds: List<EntityId>,
        causedByControllerId: EntityId?
    ): GameState {
        if (causedByControllerId == null || cardIds.isEmpty()) return state
        return state.copy(
            pendingDiscardCauseControllers = state.pendingDiscardCauseControllers +
                cardIds.associateWith { causedByControllerId }
        )
    }

    /**
     * Move multiple cards from a player's hand to their graveyard as a single discard.
     *
     * Emits one combined `CardsDiscardedEvent` (so the client renders "You discarded X, Y"
     * as a single log entry) plus one `ZoneChangeEvent` per card from `moveToZone`.
     *
     * The discard event fires whichever zone the cards actually end up in: a card whose own
     * replacement diverts it (Wilt-Leaf Liege onto the battlefield, madness into exile) has still
     * been discarded, and discard triggers still see it.
     *
     * **This is the only discard path.** Routing every site here — costs, cycling, cleanup,
     * effects — is what makes a card-intrinsic discard replacement (CR 702.35a madness, CR 614.12
     * Wilt-Leaf Liege) hold everywhere rather than only where someone remembered to check. Sites
     * that hand-rolled `removeFromZone(hand) + addToZone(graveyard)` silently bypassed both.
     *
     * @param causedByControllerId Controller of the spell or ability causing the discard, via
     *   [markDiscardCause]. Null for the cleanup-step hand-size discard and for cost payments.
     * @param asCyclingCost Marks the emitted [CardsDiscardedEvent] as a cycling cost payment
     *   (CR 702.29a) so the client can suppress the duplicate log line. Triggers ignore it.
     */
    fun discardCards(
        state: GameState,
        playerId: EntityId,
        cardIds: List<EntityId>,
        causedByControllerId: EntityId? = null,
        asCyclingCost: Boolean = false
    ): ZoneTransitionResult {
        if (cardIds.isEmpty()) return ZoneTransitionResult(state, emptyList())
        val cardNames = cardIds.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
        var newState = markDiscardCause(state, cardIds, causedByControllerId)
        val moveEvents = mutableListOf<EngineGameEvent>()
        for (cardId in cardIds) {
            val result = moveToZone(
                state = newState,
                entityId = cardId,
                destinationZone = Zone.GRAVEYARD,
                fromZoneKey = ZoneKey(playerId, Zone.HAND)
            )
            newState = result.state
            moveEvents.addAll(result.events)
        }
        newState = trackDiscard(newState, playerId, cardIds)
        val discardEvent = CardsDiscardedEvent(playerId, cardIds, cardNames, asCyclingCost = asCyclingCost)
        return ZoneTransitionResult(newState, listOf(discardEvent) + moveEvents)
    }

    /**
     * Central per-turn discard bookkeeping, mirroring [trackPermanentSacrifice]. Call this at every
     * discard site (alongside emitting [CardsDiscardedEvent]). Records the discarded cards' entity
     * ids on the discarding player's [CardsDiscardedThisTurnComponent], regardless of the card's
     * final zone (a discard diverted by a replacement still counts). Entity ids are stable across
     * the hand→graveyard move, so the recorded id matches the object now in the graveyard, which is
     * what the Mayhem gate ([com.wingedsheep.engine.mechanics.Mayhem]) reads.
     *
     * `cardIds.size` backs `TurnTracker.CARDS_DISCARDED`; membership backs `YouDiscardedThisCardThisTurn`.
     * Cleared per-player at the start of each turn by `TurnManager`.
     */
    fun trackDiscard(state: GameState, playerId: EntityId, cardIds: List<EntityId>): GameState {
        if (cardIds.isEmpty()) return state
        return state.updateEntity(playerId) { container ->
            val prior = container.get<CardsDiscardedThisTurnComponent>() ?: CardsDiscardedThisTurnComponent()
            container.with(
                prior.copy(cardIds = prior.cardIds + cardIds, count = prior.count + cardIds.size)
            )
        }
    }

    /**
     * Remove [cardId] from the discarded-this-turn *gate* list (not the monotonic count) on any
     * player who still has it recorded. Called when the card leaves a graveyard (CR 400.7 — a later
     * graveyard return is a new object that was not discarded), so a Mayhem spell can't be recast
     * each time it resolves back. A no-op if no player has the id recorded.
     */
    fun untrackDiscardedCard(state: GameState, cardId: EntityId): GameState {
        var newState = state
        for (playerId in state.turnOrder) {
            val comp = newState.getEntity(playerId)?.get<CardsDiscardedThisTurnComponent>() ?: continue
            if (cardId !in comp.cardIds) continue
            newState = newState.updateEntity(playerId) { container ->
                container.with(comp.copy(cardIds = comp.cardIds - cardId))
            }
        }
        return newState
    }

    /**
     * Central per-turn sacrifice bookkeeping. Call this at every sacrifice site (alongside
     * emitting [PermanentsSacrificedEvent], before moving the permanents to the graveyard).
     *
     * Two effects:
     *  - Increments the turn-scoped [GameState.permanentsSacrificedThisTurn] counter by the
     *    number of permanents sacrificed (feeds [CostReductionSource.PermanentsSacrificedThisTurn]
     *    on The Balrog, Durin's Bane). Not controller-scoped: it counts every sacrifice this turn.
     *  - Increments the controller's per-player [PermanentsSacrificedThisTurnComponent] by the
     *    same count (controller-scoped, backs `TurnTracker.PERMANENTS_SACRIFICED` —
     *    Sawblade Skinripper).
     *  - Marks the controller with [SacrificedFoodThisTurnComponent] if any sacrificed permanent
     *    was a Food (Food-sacrifice triggers, e.g. Ygra).
     *  - Marks the controller with [SacrificedArtifactThisTurnComponent] if any sacrificed
     *    permanent was an artifact (backs `TurnTracker.ARTIFACT_SACRIFICED` — Suspicious
     *    Detonation, Furtive Courier).
     *
     * Both markers read the *projected* characteristics, so a permanent that was only a Food or
     * only an artifact through a continuous effect still counts, and both are checked
     * independently — one sacrifice can set both.
     */
    fun trackPermanentSacrifice(state: GameState, permanentIds: List<EntityId>, controllerId: EntityId): GameState {
        if (permanentIds.isEmpty()) return state
        var newState = state.copy(
            permanentsSacrificedThisTurn = state.permanentsSacrificedThisTurn + permanentIds.size,
            // Mark these as being sacrificed so the imminent moveToZone stamps wasSacrificed
            // on each ZoneChangeEvent (CR 701.21 — read by Urza's Miter et al.).
            pendingSacrificeIds = state.pendingSacrificeIds + permanentIds,
        )
        newState = newState.updateEntity(controllerId) { container ->
            val prior = container.get<PermanentsSacrificedThisTurnComponent>()?.count ?: 0
            container.with(PermanentsSacrificedThisTurnComponent(prior + permanentIds.size))
        }
        val projected = state.projectedState
        val sacrificedCards = permanentIds.filter { newState.getEntity(it)?.has<CardComponent>() == true }
        if (sacrificedCards.any { projected.hasSubtype(it, Subtype.FOOD.value) }) {
            newState = newState.updateEntity(controllerId) { container ->
                container.with(SacrificedFoodThisTurnComponent)
            }
        }
        if (sacrificedCards.any { projected.hasType(it, CardType.ARTIFACT.name) }) {
            newState = newState.updateEntity(controllerId) { container ->
                container.with(SacrificedArtifactThisTurnComponent)
            }
        }
        return newState
    }

    // ── Private helpers ──

    /**
     * Apply battlefield entry components to an entity.
     */
    private fun applyBattlefieldEntry(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        controllerId: EntityId,
        options: ZoneEntryOptions,
        fromZone: Zone? = null
    ): GameState {
        val withEntity = state.updateEntity(entityId) { c ->
            var updated = c.with(ControllerComponent(controllerId))

            // Clear stale LinkedExileComponent from previous battlefield visit (Rule 400.7:
            // a permanent that re-enters the battlefield is a new object with no memory of
            // its previous existence, so it should not retain links to previously exiled cards)
            updated = updated.without<LinkedExileComponent>()
            updated = updated.without<com.wingedsheep.engine.state.components.battlefield.NotedExileComponent>()

            // Same for CraftedFromExiledComponent (CR 702.167c materials link): the
            // re-entering object is a new object, so it has no recorded materials. The
            // craft-return executor explicitly re-attaches the component immediately after
            // this entry path runs.
            updated = updated.without<CraftedFromExiledComponent>()

            // Track that this permanent entered the battlefield this turn
            updated = updated.with(EnteredThisTurnComponent)

            // Track reanimation (direct graveyard → battlefield) for triggers that care.
            if (fromZone == Zone.GRAVEYARD) {
                updated = updated.with(
                    com.wingedsheep.engine.state.components.battlefield.EnteredFromGraveyardComponent
                )
            }

            // Same for a direct exile → battlefield entry (a blink returning it, or any "put an
            // exiled card onto the battlefield" effect) — Extraordinary Journey.
            if (fromZone == Zone.EXILE) {
                updated = updated.with(
                    com.wingedsheep.engine.state.components.battlefield.EnteredFromExileComponent
                )
            }

            // All permanents enter summoning sick (CR 302.6 / 508.1a — the control-continuity
            // check is about the permanent, not whether it was a creature the whole turn).
            // Downstream checks gate on isCreature/{T}-cost so this is a no-op for lands and
            // non-creature artifacts until they become creatures (Crew, animate-land, etc.).
            updated = updated.with(SummoningSicknessComponent)

            // Tapped entry
            if (options.tapped || options.tappedAndAttacking) {
                updated = updated.with(TappedComponent)
            }

            // Tapped and attacking
            if (options.tappedAndAttacking) {
                val defenderId = state.turnOrder.firstOrNull { it != controllerId }
                if (defenderId != null) {
                    updated = updated.with(AttackingComponent(defenderId))
                }
            }

            // Face-down entry (morph / manifest / disguise / cloak)
            if (options.faceDown) {
                updated = updated.with(FaceDownComponent)
                if (options.morphData != null) {
                    updated = updated.with(options.morphData)
                }
                if (options.faceDownMode != null) {
                    updated = updated.with(FaceDownModeComponent(options.faceDownMode))
                }
            }

            // Register static abilities (continuous effects) and runtime replacement effects.
            // Without this, a permanent placed on the battlefield via moveToZone — leyline
            // starts, reanimation, returns from exile — would carry its CardComponent but
            // never surface its static / replacement payload to the projector or the
            // replacement-application paths. Face-down entries are excluded because face-down
            // permanents have no abilities (CR 708.2). The cast pipeline owns its own call,
            // so this does not double-run for cast spells.
            if (!options.faceDown && ::staticAbilityHandler.isInitialized) {
                updated = staticAbilityHandler.addContinuousEffectComponent(updated)
                updated = staticAbilityHandler.addReplacementEffectComponent(updated)
            }

            updated
        }

        // Rule 712 face tracking for every *non-cast* battlefield entry (reanimation, a fetch that
        // puts the card onto the battlefield, a return from exile). The cast pipeline stamps its own
        // DoubleFacedComponent as the permanent spell resolves; nothing stamped it here, so a
        // double-faced card that arrived by any other route could not be turned over at all. Face-down
        // entries are excluded: a face-down permanent has no characteristics to flip between (CR 708.2).
        // (Playing a land bypasses this whole method, so PlayLandHandler makes the same call itself.)
        val withDfcEntry = if (!options.faceDown && ::cardRegistry.isInitialized) {
            stampDoubleFacedFrontFace(withEntity, cardRegistry, entityId)
        } else {
            withEntity
        }

        val withDayboundEntry = if (!options.faceDown && ::cardRegistry.isInitialized) {
            DayNightService.applyDayboundEntry(withDfcEntry, cardRegistry, entityId)
        } else {
            withDfcEntry
        }

        // "Lands you control enter untapped" (The Wandering Minstrel): an EntersUntapped effect on
        // another battlefield permanent overrides a tapped entry from an effect that put this
        // permanent onto the battlefield tapped (ramp/fetch). Checked after the entity is fully
        // placed so its controller/type are visible to the filter. (tappedAndAttacking — combat
        // tokens — is intentionally not overridden; its filter never matches a land anyway.)
        val entersUntapped = EnterUntappedReplacements.entersUntapped(
            withDayboundEntry,
            entityId,
            controllerId
        )
        // The entering card's OWN printed "this permanent enters tapped" clause. The cast path
        // (StackResolver) and the land-play path (PlayLandHandler) read it themselves because
        // neither routes through this method; every *other* card-based entry — reanimation, a
        // return from exile, a search-library move — arrives here, and until this call each of
        // them entered untapped no matter what the card said.
        val selfEntersTapped = !options.faceDown &&
            selfEntersTapped(withDayboundEntry, entityId, controllerId)
        val withTapResolved = when {
            // A tapped entry — asked for by the effect (ramp/fetch) or printed on the card itself
            // — overridden by "enters untapped" (The Wandering Minstrel). Two applicable entry
            // replacements leave the order to the permanent's controller (CR 616.1e); the engine
            // always resolves this pair to untapped, matching the cast path and the token path
            // (EnterTappedReplacements.applyCreatedTokenEntryTap).
            (options.tapped || selfEntersTapped) && !options.tappedAndAttacking && entersUntapped ->
                withDayboundEntry.updateEntity(entityId) { it.without<TappedComponent>() }
            // An untapped entry forced tapped by the card's own clause, or by a global
            // "[filter] enter tapped" (Zhao, the Moon Slayer's "Nonbasic lands enter tapped").
            // Gated on !entersUntapped so an "enters untapped" replacement still wins (CR 614).
            !options.tapped && !entersUntapped &&
                (
                    selfEntersTapped ||
                        EnterTappedReplacements.entersTapped(withDayboundEntry, entityId, controllerId)
                    ) ->
                withDayboundEntry.updateEntity(entityId) { it.with(TappedComponent) }
            else -> withDayboundEntry
        }

        // Track "a permanent entered the battlefield face down under your control this turn"
        // (Oblivious Bookworm). Per-player count, cleared at the turn boundary by
        // CleanupPhaseManager.
        if (!options.faceDown) return withTapResolved
        return withTapResolved.updateEntity(controllerId) { playerContainer ->
            val existing = playerContainer.get<PermanentEnteredFaceDownThisTurnComponent>()
                ?: PermanentEnteredFaceDownThisTurnComponent()
            playerContainer.with(PermanentEnteredFaceDownThisTurnComponent(existing.count + 1))
        }
    }

    /**
     * Whether the entering permanent's **own** printed "[this permanent] enters tapped" clause
     * applies to this entry — a continuous replacement effect the object carries about itself
     * (CR 614.1d), applied as it enters (CR 614.12: "Such effects may come from the permanent
     * itself if they affect only that permanent").
     *
     * Three printed shapes, and only two of them can be decided from a pure state transition:
     *  - **plain** ("This land enters tapped.") — always applies;
     *  - **`unlessCondition`** ("… unless you control two or more other lands.") — applies when the
     *    condition evaluates **false**, the same polarity the cast path uses in `StackResolver`;
     *  - **`payLifeCost`** (shock lands: "… unless you pay 2 life.") — a *player decision*, and
     *    [moveToZone] has nowhere to pause. It resolves fail-closed to **tapped**: the outcome a
     *    player who declines to pay gets. Offering the choice needs a continuation in the two
     *    off-stack executors (`MoveToZoneEffectExecutor`, `MoveCollectionExecutor`) the way
     *    `StackResolver` offers it for a resolving permanent spell; until then a reanimated or
     *    fetched shock land enters tapped without being asked, rather than silently untapped as it
     *    did before this method consulted the clause at all.
     *
     * The card definition is re-read from [state] rather than taken from the caller's snapshot so
     * a double-faced card that reverted to its front face on the way in is asked about the face
     * that is actually entering. Face-down entries never reach here — a face-down permanent has
     * no abilities (CR 708.2).
     */
    private fun selfEntersTapped(
        state: GameState,
        entityId: EntityId,
        controllerId: EntityId,
    ): Boolean {
        if (!::cardRegistry.isInitialized) return false
        val cardDefinitionId = state.getEntity(entityId)?.get<CardComponent>()?.cardDefinitionId
            ?: return false
        val cardDef = cardRegistry.getCard(cardDefinitionId) ?: return false
        val entersTapped = cardDef.script.replacementEffects
            .filterIsInstance<EntersTapped>()
            .firstOrNull()
            ?: return false
        val unlessCondition = entersTapped.unlessCondition ?: return true
        return !conditionEvaluator.evaluate(
            state,
            unlessCondition,
            EffectContext(sourceId = entityId, controllerId = controllerId)
        )
    }

    /**
     * Place a card in the library according to the LibraryPlacement strategy.
     */
    private fun placeInLibrary(
        state: GameState,
        entityId: EntityId,
        libraryZoneKey: ZoneKey,
        placement: LibraryPlacement
    ): GameState {
        val currentLibrary = state.getZone(libraryZoneKey)
        return when (placement) {
            LibraryPlacement.Top -> {
                state.copy(zones = state.zones + (libraryZoneKey to listOf(entityId) + currentLibrary))
            }
            LibraryPlacement.Bottom -> {
                state.copy(zones = state.zones + (libraryZoneKey to currentLibrary + entityId))
            }
            LibraryPlacement.Shuffled -> {
                val (newLibrary, shuffledState) = state.nextRandom { shuffle(currentLibrary + entityId) }
                shuffledState.copy(zones = shuffledState.zones + (libraryZoneKey to newLibrary))
            }
            is LibraryPlacement.NthFromTop -> {
                val insertIndex = placement.position.coerceAtMost(currentLibrary.size)
                val newLibrary = currentLibrary.toMutableList().apply { add(insertIndex, entityId) }
                state.copy(zones = state.zones + (libraryZoneKey to newLibrary))
            }
        }
    }

    // ── Cleanup helpers (moved from ZoneMovementUtils) ──

    /**
     * Apply Saga entry setup to an entity entering the battlefield (Rule 714.3a).
     */
    private fun applySagaEntryIfNeeded(
        state: GameState,
        entityId: EntityId
    ): Pair<GameState, List<EngineGameEvent>> {
        return ZoneMovementUtils.applySagaEntryIfNeeded(state, entityId)
    }

    /**
     * Place a permanent's intrinsic entry counters as it enters the battlefield — a planeswalker's
     * printed loyalty (CR 306.5b) or a battle's printed defense (CR 310.4b).
     */
    private fun applyIntrinsicEntryCountersIfNeeded(
        state: GameState,
        entityId: EntityId,
        controllerId: EntityId,
        registry: CardRegistry
    ): Pair<GameState, List<EngineGameEvent>> {
        return ZoneMovementUtils.applyIntrinsicEntryCountersIfNeeded(state, entityId, controllerId, registry)
    }

    /**
     * Clean up combat references to a leaving entity on other creatures.
     */
    private fun cleanupCombatReferences(state: GameState, entityId: EntityId): GameState {
        return ZoneMovementUtils.cleanupCombatReferences(state, entityId)
    }

    /**
     * Remove floating effects targeting an entity leaving the battlefield (Rule 400.7).
     */
    private fun removeFloatingEffectsTargeting(state: GameState, entityId: EntityId): GameState {
        return ZoneMovementUtils.removeFloatingEffectsTargeting(state, entityId)
    }

    /**
     * Clean up the reverse attachment link on the permanent this entity was attached to.
     */
    private fun cleanupReverseAttachmentLink(state: GameState, entityId: EntityId): GameState {
        return ZoneMovementUtils.cleanupReverseAttachmentLink(state, entityId)
    }

    /**
     * Strip all battlefield-specific components from an entity leaving the battlefield.
     */
    private fun stripBattlefieldComponents(container: ComponentContainer): ComponentContainer {
        return ZoneMovementUtils.stripBattlefieldComponents(container)
    }

    /**
     * Build a TypeLine that reflects the projected types/subtypes for a permanent
     * leaving the battlefield. This lets leaves-battlefield triggers match on types
     * granted by continuous effects (e.g., "whenever a Food is put into a graveyard"
     * firing for creatures Ygra turned into Food artifacts).
     *
     * Falls back to the base typeLine if projection has no entry for the entity.
     *
     * Delegates to the shared
     * [com.wingedsheep.engine.state.components.stack.projectedTypeLine] so the cost-time capture
     * (`ActivateAbilityHandler`'s `lastKnownSourceSnapshot`) freezes exactly the same type line
     * this path does — one implementation, not two that can drift.
     */
    private fun buildProjectedTypeLine(
        cardComponent: CardComponent,
        state: GameState,
        entityId: EntityId
    ): TypeLine = com.wingedsheep.engine.state.components.stack.projectedTypeLine(
        state, entityId, cardComponent.typeLine
    )

    /**
     * Find which zone an entity is currently in.
     */
    private fun findEntityZone(state: GameState, entityId: EntityId): ZoneKey? {
        for ((zoneKey, entities) in state.zones) {
            if (entityId in entities) {
                return zoneKey
            }
        }
        return null
    }
}
