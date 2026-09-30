package com.wingedsheep.engine.handlers.actions.spell

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.mechanics.MayhemGrants
import com.wingedsheep.engine.mechanics.WarpGrants
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.CastSpellRecord
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CastFromTopOfLibraryUsesThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.MayCastFromGraveyardUsedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.MayCastFromLinkedExileUsedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.MayCastWithoutPayingCostUsedThisTurnComponent
import com.wingedsheep.engine.state.components.identity.AfterResolveDestinationComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.CardsDiscardedThisTurnComponent
import com.wingedsheep.engine.state.components.stack.GraveyardCastRiderComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * The cast permissions that authorized this cast, captured while the card is still in its origin
 * zone — once it moves to the stack, the grants that point at it there can no longer be found.
 */
internal class CastAuthorization(
    /** The linked-exile granter (Maralen, Intrepid Paleontologist), for its once-per-turn mark and entry rider. */
    val linkedExileGranter: com.wingedsheep.engine.legality.LegalityKernel.LinkedExileGranter?,
    /** A limited "cast from the top of your library" source whose use this cast consumes. */
    val limitedTopLibraryCastSource: EntityId?,
    /** The Tomb of Aclazotz / Bilbo: the rider-bearing graveyard-cast grant that authorized this cast. */
    val graveyardCastRiderGrant: com.wingedsheep.sdk.scripting.MayCastFromGraveyard?,
)

/**
 * Which alternative cost this cast was paid with, for the mechanics whose consequences outlive the
 * cast (a warped permanent is exiled at end step, an evoked one sacrificed, …). Each is gated by the
 * chosen [AlternativeCostType], so when two alternative costs collide only the chosen one drives its
 * behaviour; with no choice recorded it falls back to "the card has that keyword".
 */
internal class AlternativeCostMarks(
    val wasWarped: Boolean,
    val wasDashed: Boolean,
    val wasEvoked: Boolean,
    val wasImpending: Boolean,
    val wasCleaved: Boolean,
    val wasMayhem: Boolean,
)

/**
 * The records a cast leaves behind once it is paid for (CR 601.2i and the trackers the game keeps
 * about spells cast): the turn's spell counts and cast history, the once-per-turn marks on the
 * permissions it used, and the cast-this-way riders frozen onto the spell for resolution.
 */
internal class CastRecords(
    private val cardRegistry: CardRegistry,
    private val zoneResolver: CastZoneResolver,
    private val costCalculator: CostCalculator,
    private val castCostTotaller: CastCostTotaller,
    private val stackResolver: StackResolver,
    private val predicateEvaluator: PredicateEvaluator,
) {

    fun captureAuthorization(state: GameState, action: CastSpell, cardComponent: CardComponent) = CastAuthorization(
        linkedExileGranter = zoneResolver.findLinkedExileGranterEntry(state, action.playerId, action.cardId),
        limitedTopLibraryCastSource = if (action.cardId in state.getLibrary(action.playerId)) {
            zoneResolver.findLimitedTopLibraryCastSourceToConsume(state, action.playerId, action.cardId)
        } else null,
        graveyardCastRiderGrant = zoneResolver.findMayCastFromGraveyardGrant(
            state, action.playerId, action.cardId, cardComponent, action.graveyardCastRider
        ),
    )

    /**
     * Reads which alternative cost was paid. Mayhem also drops the card's "discarded this turn"
     * mark: the card is leaving the graveyard to become a spell (CR 400.7 — a new object), and
     * casting bypasses `ZoneTransitionService.moveToZone`, so without this it could be mayhem-cast
     * again each time it resolves back.
     */
    fun markAlternativeCost(state: GameState, action: CastSpell, cardDef: CardDefinition?): Pair<GameState, AlternativeCostMarks> {
        fun paidWith(type: AlternativeCostType) = action.useAlternativeCost && cardDef != null && action.altAllows(type)
        fun hasKeyword(type: AlternativeCostType, keyword: (KeywordAbility) -> Boolean) =
            paidWith(type) && cardDef!!.keywordAbilities.any(keyword)

        // Mayhem (CR 702.187): the card actually has mayhem and the "you discarded this card this
        // turn" record holds (zone-independent, so it still reads after the card moves to the
        // stack). Drives Sandman's Quicksand's "if this spell's mayhem cost was paid" rider.
        val wasMayhem = paidWith(AlternativeCostType.MAYHEM) &&
            MayhemGrants.effectiveMayhem(state, action.cardId, cardDef!!, action.playerId, cardRegistry, predicateEvaluator) != null &&
            state.getEntity(action.playerId)?.get<CardsDiscardedThisTurnComponent>()?.cardIds?.contains(action.cardId) == true
        val afterMayhem = if (wasMayhem) ZoneTransitionService.untrackDiscardedCard(state, action.cardId) else state

        return afterMayhem to AlternativeCostMarks(
            // Warp can also be granted to cards in hand by a battlefield static, hence the grant lookup.
            wasWarped = paidWith(AlternativeCostType.WARP) &&
                WarpGrants.effectiveWarp(state, action.cardId, cardDef!!, action.playerId, cardRegistry, predicateEvaluator) != null,
            // Dash (CR 702.109): printed-only for now — no granted-dash resolver exists yet.
            wasDashed = hasKeyword(AlternativeCostType.DASH) { it is KeywordAbility.Dash },
            wasEvoked = hasKeyword(AlternativeCostType.EVOKE) { it is KeywordAbility.Evoke },
            wasImpending = hasKeyword(AlternativeCostType.IMPENDING) { it is KeywordAbility.Impending },
            // Cleave (CR 702.148): the spell resolves with its brackets-removed effect/target
            // variant instead of its printed one.
            wasCleaved = hasKeyword(AlternativeCostType.CLEAVE) { it is KeywordAbility.Cleave },
            wasMayhem = wasMayhem,
        )
    }

    /**
     * Counts the spell toward the turn's totals (storm, "second spell each turn") and appends it to
     * the caster's cast history (Relic Runner's evasion, "first of type" triggers). Returns the
     * state and the storm count — the number of spells cast before this one.
     */
    fun recordSpellCast(
        state: GameState,
        action: CastSpell,
        cardDef: CardDefinition?,
        cardComponent: CardComponent,
        transformedFace: com.wingedsheep.sdk.model.CardDefinition?,
        modalBackFace: com.wingedsheep.sdk.model.CardDefinition?,
        wasWarped: Boolean,
        payment: PaymentResult,
    ): Pair<GameState, Int> {
        val stormCount = state.spellsCastThisTurn
        val playerCount = state.playerSpellsCastThisTurn[action.playerId] ?: 0
        var newState = state.copy(
            spellsCastThisTurn = stormCount + 1,
            playerSpellsCastThisTurn = state.playerSpellsCastThisTurn + (action.playerId to playerCount + 1),
            spellWarpedThisTurn = state.spellWarpedThisTurn || wasWarped
        )

        val record = CastSpellRecord(
            // A transformed cast is on the stack back face up, so "a Spirit spell was cast" and
            // colour/type history read the back face (CR 712.8c / 712.8f). Mana value splits by
            // route: a disturb cast keeps the front face's (CR 712.8c), which is what
            // `cardComponent` still holds here, while CR 712.8f gives a modal double-faced spell
            // "only the characteristics of the face that's up" with no such exception — so a
            // back-face cast reports that face's own mana value. Mirrors `StackResolver`'s
            // `spellManaValue`, which stamps the same number onto the SpellCastEvent.
            typeLine = (transformedFace?.typeLine ?: cardComponent.typeLine).let { printed ->
                // A cross-zone GrantCardType (Encroaching Mycosynth) already reaches the card in its
                // origin zone, so history records the spell as the type it is cast as.
                val granted = state.projectedState.crossZoneGrantedCardTypes(action.cardId)
                    .mapNotNull { name -> CardType.entries.firstOrNull { it.name == name } }
                if (granted.isEmpty()) printed else printed.copy(cardTypes = printed.cardTypes + granted)
            },
            manaValue = modalBackFace?.manaCost?.cmc ?: cardComponent.manaValue,
            colors = transformedFace?.colors ?: cardComponent.colors,
            isFaceDown = action.castFaceDown,
            spentManaSubtypes = payment.spentManaProvenance.spentSubtypes,
            // The cast card moves to the stack keeping its entity id, so this matches the resolving
            // spell's EffectContext.sourceId (used by SpellsCastThisTurn excludeSelf).
            sourceEntityId = action.cardId,
            // Origin zone of the cast (HAND for a normal cast; GRAVEYARD/EXILE/COMMAND for
            // flashback/forage, plot/foretell, commander, …). The card is still in its origin zone
            // here — stackResolver.castSpell moves it later — so this resolves the same way
            // castSpell stamps SpellOnStackComponent.castFromZone. Powers "you haven't cast a spell
            // from your hand this turn" (Prairie Dog cycle).
            castFromZone = stackResolver.findCastFromZone(newState, action.cardId, action.playerId),
            // Face-down casts hide the card's identity; a face-up cast records the name so name
            // predicates ("the first Otter spell other than Alania") can match history.
            name = if (action.castFaceDown) null else (
                action.faceIndex?.let { cardDef?.cardFaces?.getOrNull(it)?.name }
                    ?: transformedFace?.name ?: cardComponent.name
            ),
        )
        val existing = newState.spellsCastThisTurnByPlayer[action.playerId] ?: emptyList()
        newState = newState.copy(
            spellsCastThisTurnByPlayer = newState.spellsCastThisTurnByPlayer + (action.playerId to existing + record),
            // "the spell most recently cast this turn" — read by Mana Maze's cast restriction.
            lastCastSpellColors = record.colors
        )
        return newState to stormCount
    }

    /**
     * Freezes a cast-this-way rider onto the stack spell; StackResolver reads it back when the spell
     * resolves.
     *
     * The entry rider (a finality counter and/or an added subtype when the permanent enters) has
     * three sources:
     *  - The Tomb of Aclazotz: a rider-bearing MayCastFromGraveyard grant (finality counter + added
     *    subtype), from the specific grant that authorized this cast.
     *  - Osteomancer Adept's forage permission: "that creature enters with a finality counter on it"
     *    (finality only, no added subtype) — reusing the same entry-rider plumbing.
     *  - Intrepid Paleontologist: a rider-bearing GrantMayCastFromLinkedExile ("If you cast a spell
     *    this way, that creature enters with a finality counter on it") — same plumbing, but the
     *    authorizing grant is the linked-exile cast permission captured pre-cast.
     *
     * Bilbo, Thief in the Night supplies the instant/sorcery half of the family — "if an instant or
     * sorcery spell cast this way would be put into your graveyard, exile it instead". Scoped to the
     * *specific* grant that authorized this cast, so a simultaneous graveyard-cast permission from
     * another source is unaffected. `onlyIfResolved = false` because the replacement catches the
     * countered/fizzled spell too (printed ruling: an Adventure spell that fails to resolve is still
     * exiled by this effect).
     */
    fun applyCastThisWayRiders(
        state: GameState,
        action: CastSpell,
        cardComponent: CardComponent,
        authorization: CastAuthorization,
        isForageCast: Boolean,
    ): GameState {
        var newState = state
        val grant = authorization.graveyardCastRiderGrant
        val riderCounter: CounterType? = when {
            grant?.hasEntryRider == true -> grant.entersWithCounter
            isForageCast -> CounterType.FINALITY
            authorization.linkedExileGranter?.ability?.entersWithCounter != null ->
                authorization.linkedExileGranter.ability.entersWithCounter
            else -> null
        }
        val riderSubtype: String? = grant?.takeIf { it.hasEntryRider }?.addedSubtypeOnEntry
        if (riderCounter != null || riderSubtype != null) {
            newState = newState.updateEntity(action.cardId) { c ->
                c.with(GraveyardCastRiderComponent(entersWithCounter = riderCounter, addedSubtype = riderSubtype))
            }
        }
        if (grant?.exileInsteadOfGraveyard == true && cardComponent.typeLine.let { it.isInstant || it.isSorcery }) {
            newState = newState.updateEntity(action.cardId) { c ->
                c.with(AfterResolveDestinationComponent(onlyIfResolved = false))
            }
        }
        return newState
    }

    /**
     * Marks each limited cast permission this cast used, so it can't be used again this turn. Each
     * is resolved against [originState] (the state before the cast), since the card has already left
     * the zone the permission was found through.
     *
     * - Muldrotha: one permanent type per turn, recorded when [castingFromGraveyardViaMuldrotha].
     * - A once-per-turn linked-exile permission (Maralen, Fae Ascendant) marks its granter.
     * - A limited top-of-library permission belongs to its granting permanent, not to the player: the
     *   source captured before the card left the library is marked; a source that leaves and returns
     *   is a new object with a fresh allowance, and an unlimited matching source consumes nothing.
     * - A `MayCastWithoutPayingManaCost(oncePerTurn = true)` source (Zaffai and the Tempests) is
     *   consumed only when no unlimited free-cast source could have paid instead; the origin zone
     *   decides whether a `fromExileOnly` source (Warped Space) applied at all.
     * - A once-per-turn graveyard-cast grant (Gisa and Geralf) is burned only when no unlimited grant
     *   could have authorized the cast.
     */
    fun consumeCastPermissions(
        state: GameState,
        originState: GameState,
        action: CastSpell,
        cardDef: CardDefinition?,
        cardComponent: CardComponent,
        authorization: CastAuthorization,
        castingFromGraveyardViaMuldrotha: Boolean,
    ): GameState {
        var newState = state
        if (castingFromGraveyardViaMuldrotha) {
            val typeName = zoneResolver.choosePermanentTypeForGraveyardPermission(newState, action.playerId, cardComponent)
            if (typeName != null) {
                newState = zoneResolver.recordGraveyardPlayPermissionUsage(newState, action.playerId, typeName)
            }
        }

        val linkedExileGranter = authorization.linkedExileGranter
        if (linkedExileGranter?.ability?.oncePerTurn == true) {
            newState = newState.updateEntity(linkedExileGranter.granterId) { c -> c.with(MayCastFromLinkedExileUsedThisTurnComponent) }
        }

        authorization.limitedTopLibraryCastSource?.let { source ->
            newState = newState.updateEntity(source) { c ->
                val tracker = c.get<CastFromTopOfLibraryUsesThisTurnComponent>()
                c.with(CastFromTopOfLibraryUsesThisTurnComponent(uses = (tracker?.uses ?: 0) + 1))
            }
        }

        val castFromZone = castCostTotaller.castSourceZone(originState, action.cardId)
        if (action.useWithoutPayingManaCost) {
            costCalculator.oncePerTurnFreeCastSourceToConsume(newState, action.playerId, cardDef, castFromZone)?.let { source ->
                newState = newState.updateEntity(source) { c -> c.with(MayCastWithoutPayingCostUsedThisTurnComponent) }
            }
        }

        if (castFromZone == Zone.GRAVEYARD) {
            zoneResolver.oncePerTurnGraveyardCastSourceToConsume(originState, action.playerId, action.cardId)?.let { source ->
                newState = newState.updateEntity(source) { c -> c.with(MayCastFromGraveyardUsedThisTurnComponent) }
            }
        }
        return newState
    }

    /** Casting from a graveyard through Muldrotha's MayPlayPermanentsFromGraveyard. */
    fun isCastViaMuldrotha(state: GameState, action: CastSpell, cardComponent: CardComponent): Boolean =
        action.cardId in state.getZone(ZoneKey(action.playerId, Zone.GRAVEYARD)) &&
            zoneResolver.hasMayPlayPermanentFromGraveyardPermission(state, action.playerId, action.cardId, cardComponent)
}
