package com.wingedsheep.engine.handlers.actions.spell

import com.wingedsheep.engine.core.GraveyardCastRiderSelection
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.mechanics.DisturbCasts
import com.wingedsheep.engine.mechanics.FlashTypeGrants
import com.wingedsheep.engine.mechanics.FlashbackGrants
import com.wingedsheep.engine.mechanics.HarmonizeGrants
import com.wingedsheep.engine.mechanics.ModalDfcCasts
import com.wingedsheep.engine.mechanics.WarpGrants
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.ExileEntryTurnComponent
import com.wingedsheep.engine.state.components.battlefield.CastFromTopOfLibraryUsesThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.GraveyardPlayPermissionUsedComponent
import com.wingedsheep.engine.state.components.battlefield.MayCastFromGraveyardUsedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.MayCastFromLinkedExileUsedThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.CommanderComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.permissions.hasMayPlayFor
import com.wingedsheep.engine.state.components.player.FlashGrantsThisTurnComponent
import com.wingedsheep.engine.state.components.player.MayCastCreaturesFromGraveyardWithForageComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.MayPlayPermanentsFromGraveyard
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Resolves where a card can be cast from and what permissions apply.
 *
 * Handles the complex logic of determining whether a card can be cast from
 * non-hand zones (library top, exile, graveyard) based on static abilities
 * and components on the game state.
 */
class CastZoneResolver(
    private val cardRegistry: CardRegistry,
    private val conditionEvaluator: ConditionEvaluator
) {
    private val predicateEvaluator = PredicateEvaluator()

    /**
     * Check if a card is on top of the player's library and the player controls
     * a permanent with PlayFromTopOfLibrary (e.g., Future Sight) or
     * CastSpellTypesFromTopOfLibrary (e.g., Precognition Field).
     */
    fun isOnTopOfLibraryWithPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val library = state.getLibrary(playerId)
        if (library.isEmpty() || library.first() != cardId) return false
        if (hasPlayFromTopOfLibrary(state, playerId)) return true
        return hasCastFromTopOfLibraryPermission(state, playerId, cardId)
    }

    /**
     * Check if a card is in a non-hand "other" zone and has an active `MayPlayPermission`
     * granting the player permission to play it.
     *
     * Covers exile / graveyard / library:
     * - Exile + graveyard: free-cast grants like Etali, Mind's Desire, Malcolm.
     * - Library: cards revealed by an effect like Sunbird's Invocation, where the oracle
     *   text leaves the cards in the library while permitting a free cast directly from
     *   among the revealed pile. CR semantics treat reveal-then-cast as casting from the
     *   library; the MayPlayPermission gates which specific cards qualify.
     *
     * Checks all players' exile/graveyard zones because cards like Villainous Wealth
     * exile from an opponent's library (cards remain in their owner's zone but are
     * castable by the spell's controller). Library coverage is restricted to the
     * controller's own library — there's no current effect that grants free cast from
     * an opponent's library while it stays there.
     *
     * Also checks for permanents with `GrantMayCastFromLinkedExile` static ability
     * (e.g., Rona, Disciple of Gix) that link exiled cards via `LinkedExileComponent`.
     */
    fun isInExileWithPlayPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val inOtherZone = state.turnOrder.any { pid ->
            cardId in state.getZone(ZoneKey(pid, Zone.EXILE)) ||
                cardId in state.getZone(ZoneKey(pid, Zone.GRAVEYARD))
        } || cardId in state.getZone(ZoneKey(playerId, Zone.LIBRARY))
        if (!inOtherZone) return false

        // Check direct MayPlayPermission. When the grant carries a runtime condition
        // (Possibility Technician's "if you control a Kavu"), fall through to linked-exile
        // granters when the gate is closed — those are independent permission sources and
        // may still apply.
        if (state.hasMayPlayFor(cardId, playerId, conditionEvaluator, cardRegistry)) return true

        // Check for GrantMayCastFromLinkedExile static abilities on battlefield permanents
        return hasLinkedExileCastPermission(state, playerId, cardId)
    }

    /**
     * Check if a card has an intrinsic MayCastSelfFromZones static ability
     * that permits casting from its current zone (e.g., Squee, the Immortal).
     */
    /**
     * True iff the card is in [playerId]'s command zone with `CommanderComponent` whose owner is
     * [playerId] (CR 903.8 — only the commander's owner can cast it from the command zone).
     */
    fun hasCommanderCastPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId,
    ): Boolean {
        val commandZone = ZoneKey(playerId, Zone.COMMAND)
        if (cardId !in state.getZone(commandZone)) return false
        val container = state.getEntity(cardId) ?: return false
        val commanderComponent = container.get<CommanderComponent>() ?: return false
        return commanderComponent.ownerId == playerId
    }

    fun hasMayCastSelfFromZonePermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean = findMayCastSelfFromZoneAbility(state, playerId, cardId) != null

    /**
     * Locate the [MayCastSelfFromZones] ability (if any) that currently permits casting [cardId]
     * from its present zone for [playerId] — honoring the zone match and optional [Condition] gate.
     * Callers that need the ability's [MayCastSelfFromZones.additionalCost] (e.g. Alien Symbiosis'
     * "by discarding a card") use this overload; [hasMayCastSelfFromZonePermission] is the boolean
     * convenience wrapper.
     */
    fun findMayCastSelfFromZoneAbility(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): MayCastSelfFromZones? {
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return null
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return null

        // A permission applies if the card is currently in one of its named zones for this player
        // AND its optional condition (e.g. Undead Sprinter's "a non-Zombie creature died this turn")
        // holds — mirroring the enumerator gate so the authorization can't outlive the permission.
        return cardDef.script.staticAbilities
            .filterIsInstance<MayCastSelfFromZones>()
            .firstOrNull { ability ->
                val inNamedZone = ability.zones.any { zone ->
                    cardId in state.getZone(ZoneKey(playerId, zone))
                }
                inNamedZone && (ability.condition == null ||
                    conditionEvaluator.evaluate(
                        state,
                        ability.condition!!,
                        EffectContext(sourceId = cardId, controllerId = playerId)
                    ))
            }
    }

    /**
     * Check if a permanent spell can be cast from the graveyard via a MayPlayPermanentsFromGraveyard
     * static ability (e.g., Muldrotha, the Gravetide).
     */
    fun hasMayPlayPermanentFromGraveyardPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId,
        cardComponent: CardComponent
    ): Boolean {
        // Card must be in the player's graveyard
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        if (cardId !in state.getZone(graveyardZone)) return false

        // Card must be a permanent type (not instant/sorcery)
        if (!cardComponent.typeLine.isPermanent) return false

        // Only works on controller's turn
        if (!state.isActiveTurnFor(playerId)) return false

        // Find a Muldrotha-like permanent with available permission for any of this card's types
        val permanentTypes = cardComponent.typeLine.cardTypes.filter { it.isPermanent }
        for (typeName in permanentTypes.map { it.name }) {
            if (findGraveyardPlayPermissionSource(state, playerId, typeName) != null) {
                return true
            }
        }
        return false
    }

    /**
     * Check if a card can be cast from the graveyard via a [MayCastFromGraveyard] static
     * ability (e.g., Festival of Embers' life-cost casting, or Yawgmoth's Agenda's free
     * "cast spells from your graveyard"). Matches the granting permanent's spell filter and
     * its optional during-your-turn restriction. The optional life cost is paid separately
     * (carried on the action's `graveyardLifeCost`), so it is not checked here.
     */
    fun hasMayCastFromGraveyardPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId,
        cardComponent: CardComponent
    ): Boolean = findMayCastFromGraveyardGrant(state, playerId, cardId, cardComponent) != null

    /**
     * Every [MayCastFromGraveyard] grant (printed or durationally granted) that authorizes casting
     * [cardId] from [playerId]'s graveyard right now. Empty if none applies. The graveyard-cast
     * enumerator uses this to offer one legal action per *distinct* grant (by life cost + entry
     * rider), so the player picks the permission — and thus its rider — up front (CR 601.2b).
     */
    fun applicableMayCastFromGraveyardGrants(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId,
        cardComponent: CardComponent
    ): List<MayCastFromGraveyard> {
        if (cardId !in state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))) return emptyList()
        return mayCastFromGraveyardGrantsWithSources(state, playerId, cardId).map { it.second }
    }

    /**
     * Every applicable [MayCastFromGraveyard] grant paired with the permanent it hangs off — the
     * source-aware form of [applicableMayCastFromGraveyardGrants]. The source id is what an
     * `oncePerTurn` grant's per-turn allowance is tracked against, so both the applies-check and
     * [oncePerTurnGraveyardCastSourceToConsume] read the grant through this one enumeration.
     */
    private fun mayCastFromGraveyardGrantsWithSources(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): List<Pair<EntityId, MayCastFromGraveyard>> {
        val matches = mutableListOf<Pair<EntityId, MayCastFromGraveyard>>()
        for (permId in state.getBattlefield(playerId)) {
            val permCard = state.getEntity(permId)?.get<CardComponent>() ?: continue
            val permDef = cardRegistry.getCard(permCard.cardDefinitionId) ?: continue
            for (sa in permDef.script.staticAbilities) {
                if (sa is MayCastFromGraveyard && mayCastFromGraveyardGrantApplies(state, playerId, cardId, sa, permId)) {
                    matches.add(permId to sa)
                }
            }
        }
        // Durational grants (e.g. Forgotten Cellar's "cast spells from your graveyard this turn",
        // or The Tomb of Aclazotz's per-turn creature-cast grant) recorded in grantedStaticAbilities,
        // anchored to a permanent the player controls.
        for (grant in state.grantedStaticAbilities) {
            val anchor = state.getEntity(grant.entityId) ?: continue
            val controller = anchor.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId
            if (controller != playerId) continue
            val sa = grant.ability
            if (sa is MayCastFromGraveyard &&
                mayCastFromGraveyardGrantApplies(state, playerId, cardId, sa, grant.entityId)
            ) {
                matches.add(grant.entityId to sa)
            }
        }
        return matches
    }

    /**
     * After a graveyard cast authorized by a [MayCastFromGraveyard] grant, the permanent whose
     * `oncePerTurn` allowance should be marked used — or null when an unlimited grant already
     * authorized the same cast, so no per-turn use is burned. Mirrors
     * [com.wingedsheep.engine.mechanics.mana.CostCalculator.oncePerTurnFreeCastSourceToConsume]:
     * with several once-per-turn sources, the first eligible one pays.
     */
    fun oncePerTurnGraveyardCastSourceToConsume(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): EntityId? {
        var candidate: EntityId? = null
        for ((sourceId, sa) in mayCastFromGraveyardGrantsWithSources(state, playerId, cardId)) {
            if (!sa.oncePerTurn) return null
            if (candidate == null) candidate = sourceId
        }
        return candidate
    }

    /**
     * Find the [MayCastFromGraveyard] grant that authorizes casting [cardId] from [playerId]'s
     * graveyard, or null if none applies. The returned grant's cast-this-way entry rider is frozen
     * onto the stack spell at cast time (see `CastSpellHandler`), which is why the decision is here.
     *
     * When several grants apply, [selection] (the permission the player chose at cast time, carried
     * on `CastSpell.graveyardCastRider`) disambiguates: the grant whose entry rider matches it is
     * returned, honoring the player's CR 601.2b announcement — including an explicit *no-rider*
     * choice (a free grant, when a rider-bearing one also applies). Falls back to a rider-preferring
     * auto-pick when [selection] is null (unspecified) **or** names a rider that doesn't match any
     * applicable grant — so a client can't dodge a mandatory rider by claiming a permission it lacks.
     */
    fun findMayCastFromGraveyardGrant(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId,
        cardComponent: CardComponent,
        selection: GraveyardCastRiderSelection? = null
    ): MayCastFromGraveyard? {
        val matches = applicableMayCastFromGraveyardGrants(state, playerId, cardId, cardComponent)
        if (selection != null) {
            matches.firstOrNull {
                it.entersWithCounter == selection.entersWithCounter && it.addedSubtypeOnEntry == selection.addedSubtype
            }?.let { return it }
        }
        return matches.firstOrNull { it.hasEntryRider } ?: matches.firstOrNull()
    }

    private fun mayCastFromGraveyardGrantApplies(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId,
        sa: com.wingedsheep.sdk.scripting.StaticAbility,
        grantSourceId: EntityId
    ): Boolean {
        if (sa !is MayCastFromGraveyard) return false
        if (sa.duringYourTurnOnly && !state.isActiveTurnFor(playerId)) return false
        // A `oncePerTurn` grant (Gisa and Geralf) stops authorizing casts once this specific
        // granter has been used this turn; the marker is cleared at cleanup.
        if (sa.oncePerTurn &&
            state.getEntity(grantSourceId)?.has<MayCastFromGraveyardUsedThisTurnComponent>() == true
        ) {
            return false
        }
        return predicateEvaluator.matches(
            state, state.projectedState, cardId, sa.filter,
            PredicateContext(controllerId = playerId)
        )
    }

    /**
     * Check if a card in the graveyard has a Flashback keyword ability — printed on the card or
     * granted at runtime (Archmage's Newt) — allowing it to be cast from the graveyard for its
     * flashback cost (and exiled on resolution).
     */
    fun hasFlashbackPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        if (cardId !in state.getZone(graveyardZone)) return false
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return false
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
        return FlashbackGrants.effectiveFlashback(
            state, cardId, cardDef, playerId, cardRegistry, predicateEvaluator
        ) != null
    }

    /**
     * The back face a card in [playerId]'s graveyard would be cast as through disturb (CR 702.146a),
     * or null when it isn't there, has no disturb keyword, or has no permanent back face.
     *
     * Returning the face rather than a boolean is deliberate: every disturb caller immediately needs
     * the back face's characteristics (timing from its card types, its target requirements /
     * `auraTarget`), so the permission check and the face lookup are the same question.
     */
    fun disturbCastFace(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): CardDefinition? {
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        if (cardId !in state.getZone(graveyardZone)) return null
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return null
        return DisturbCasts.castFace(cardRegistry.getCard(cardComponent.cardDefinitionId))
    }

    /**
     * The back face a card would be cast as through a **transformed** may-play permission
     * ([com.wingedsheep.engine.state.permissions.MayPlayPermission.castTransformed]), or null when
     * no such permission covers it or the card has no permanent back face.
     *
     * The permission-granted sibling of [disturbCastFace], and it returns the face for the same
     * reason: every caller needs the back face's characteristics (timing, targets, `auraTarget`)
     * because that is the face the spell has on the stack (CR 712.8c). Backs CR 310.11b's "exile
     * it, then you may cast it transformed"; unlike disturb the permission — not a printed keyword
     * — is what authorizes the cast, so the zone the card sits in is the permission's business, not
     * this lookup's.
     */
    fun permissionTransformedCastFace(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): CardDefinition? {
        val transformedGrant = state.mayPlayPermissions.any { permission ->
            permission.castTransformed &&
                permission.controllerId == playerId &&
                cardId in permission.cardIds
        }
        if (!transformedGrant) return null
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return null
        return cardRegistry.getCard(cardComponent.cardDefinitionId)?.backFace
    }

    /**
     * The back face a card in [playerId]'s **hand** would be cast as through the modal-DFC face
     * choice (CR 712.11b), or null when it isn't there or isn't a modal DFC with a permanent back
     * face.
     *
     * Mirrors [disturbCastFace] — same "hand back the face, not a boolean" contract, because every
     * caller needs the face's characteristics (timing, targets, name) right away — and differs only
     * in the zone it looks in.
     */
    fun modalBackCastFace(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): CardDefinition? {
        val handZone = ZoneKey(playerId, Zone.HAND)
        if (cardId !in state.getZone(handZone)) return null
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return null
        return ModalDfcCasts.castFace(cardRegistry.getCard(cardComponent.cardDefinitionId))
    }

    /**
     * Check if a card in the graveyard has a Harmonize keyword ability — printed on the card or
     * granted at runtime (Songcrafter Mage) — allowing it to be cast from the graveyard for its
     * harmonize cost (and exiled on resolution).
     */
    fun hasHarmonizePermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        if (cardId !in state.getZone(graveyardZone)) return false
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return false
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
        return HarmonizeGrants.effectiveHarmonize(state, cardId, cardDef) != null
    }

    /**
     * Check if a card in the graveyard has a Mayhem keyword ability — printed or granted (Green
     * Goblin's Goblin Formula) — AND you discarded it this turn (CR 702.187b), allowing it to be
     * cast from the graveyard for its mayhem cost. Unlike flashback/harmonize the spell is NOT
     * exiled on resolution.
     */
    fun hasMayhemPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        if (cardId !in state.getZone(graveyardZone)) return false
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return false
        // Lands use the no-cost "play from graveyard" form (CR 702.187c), not the cast path.
        if (cardComponent.typeLine.isLand) return false
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
        if (com.wingedsheep.engine.mechanics.MayhemGrants.effectiveMayhem(
                state, cardId, cardDef, playerId, cardRegistry, predicateEvaluator
            ) == null
        ) return false
        // The Mayhem gate: you must have discarded this card this turn.
        return state.getEntity(playerId)
            ?.get<com.wingedsheep.engine.state.components.player.CardsDiscardedThisTurnComponent>()
            ?.cardIds?.contains(cardId) == true
    }

    /**
     * Get the mayhem cost for a card, or null if it doesn't have mayhem.
     */
    fun getMayhemCost(cardId: EntityId, state: GameState): com.wingedsheep.sdk.core.ManaCost? {
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return null
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
        return com.wingedsheep.engine.mechanics.MayhemGrants.effectiveMayhem(
            state, cardId, cardDef, cardComponent.ownerId, cardRegistry, predicateEvaluator
        )?.cost
    }

    /**
     * Get the flashback cost for a card, or null if it doesn't have flashback.
     */
    fun getFlashbackCost(cardId: EntityId, state: GameState): com.wingedsheep.sdk.core.ManaCost? {
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return null
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
        // A graveyard card's controller is its owner — enough to resolve a whole-graveyard
        // group grant (Iroh, Grand Lotus) in addition to printed / per-entity flashback.
        return FlashbackGrants.effectiveFlashback(
            state, cardId, cardDef, cardComponent.ownerId, cardRegistry, predicateEvaluator
        )?.cost
    }

    /**
     * Check if a card has an active Warp keyword ability that can be used from
     * its current zone. By default warp is hand-only (CR 702.185a); a Warp whose
     * `fromGraveyard` flag is set (e.g., Timeline Culler) also lets the card be
     * cast for its warp cost from the caster's graveyard. A battlefield static
     * ability ([com.wingedsheep.sdk.scripting.GrantWarpToCardsInHand]) can also
     * grant warp to a card currently in the controller's hand.
     */
    fun hasWarpPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return false
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return false
        val warp = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Warp>().firstOrNull()
        if (warp != null) {
            if (cardId in state.getZone(ZoneKey(playerId, Zone.HAND))) return true
            if (warp.fromGraveyard && cardId in state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))) return true
        }
        return WarpGrants.hasGrantedWarpInHand(state, cardId, playerId, cardRegistry, predicateEvaluator)
    }

    /**
     * Check if a card has an active Dash keyword ability that can be used from its current
     * zone. Hand-only (CR 702.109a) — printed only for now, no granted-dash resolver exists yet
     * (mirrors [hasWarpPermission]'s shape, minus the graveyard/grant branches Warp alone has).
     */
    fun hasDashPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return false
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return false
        if (cardDef.keywordAbilities.none { it is KeywordAbility.Dash }) return false
        return cardId in state.getZone(ZoneKey(playerId, Zone.HAND))
    }

    /**
     * Check if a creature card in the graveyard can be cast via the forage permission
     * granted by `MayCastCreaturesFromGraveyardWithForageComponent` (e.g. Osteomancer Adept).
     */
    fun hasMayCastCreaturesFromGraveyardWithForage(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId,
        cardComponent: CardComponent
    ): Boolean {
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        if (cardId !in state.getZone(graveyardZone)) return false
        if (!cardComponent.typeLine.isCreature) return false
        val playerEntity = state.getEntity(playerId) ?: return false
        return playerEntity.has<MayCastCreaturesFromGraveyardWithForageComponent>()
    }

    /**
     * Check if a card has PlayWithoutPayingCostComponent granting
     * the player permission to play it without paying its mana cost.
     *
     * Also returns true when the cast is permitted by a linked-exile granter
     * whose [GrantMayCastFromLinkedExile.withoutPayingManaCost] is set
     * (e.g., Maralen, Fae Ascendant) — the granter waives the mana cost without
     * needing a per-card component.
     */
    fun hasPlayWithoutPayingCost(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val component = state.getEntity(cardId)?.get<PlayWithoutPayingCostComponent>()
        if (component?.controllerId == playerId) return true
        val granter = findLinkedExileGranter(state, playerId, cardId)
        if (granter?.withoutPayingManaCost == true) return true
        // Gwenom: a spell cast from the top of the library under a PlayFromTopWithAlternativeCost
        // permission whose withoutPayingManaCost is set pays no mana (it pays life instead).
        if (state.getLibrary(playerId).firstOrNull() == cardId) {
            return playFromTopAlternativeCost(state, playerId)?.withoutPayingManaCost == true
        }
        return false
    }

    /**
     * Check if a card has been granted flash by a GrantFlashToSpellType static ability
     * on any permanent on the battlefield (any player's battlefield), or by its own
     * conditionalFlash condition.
     */
    fun hasGrantedFlash(state: GameState, spellCardId: EntityId): Boolean {
        val spellOwner = state.getEntity(spellCardId)?.get<ControllerComponent>()?.playerId
            ?: return false

        // Check the card's own conditionalFlash (e.g., Ferocious)
        val spellCard = state.getEntity(spellCardId)?.get<CardComponent>()
        val spellDef = spellCard?.let { cardRegistry.getCard(it.cardDefinitionId) }
        val conditionalFlash = spellDef?.script?.conditionalFlash
        if (conditionalFlash != null) {
            val effectContext = EffectContext(
                sourceId = spellCardId,
                controllerId = spellOwner,
            )
            if (conditionEvaluator.evaluate(state, conditionalFlash, effectContext)) {
                return true
            }
        }

        val context = PredicateContext(controllerId = spellOwner)

        // Turn-scoped grants on the spell owner (Borne Upon a Wind etc., via
        // GrantFlashToSpellsEffect → FlashGrantsThisTurnComponent).
        val turnGrants = state.getEntity(spellOwner)?.get<FlashGrantsThisTurnComponent>()
        if (turnGrants != null) {
            for (filter in turnGrants.filters) {
                if (predicateEvaluator.matches(state, state.projectedState, spellCardId, filter, context)) {
                    return true
                }
            }
        }

        // Check GrantFlashToSpellType static abilities on battlefield permanents
        for (playerId in state.turnOrder) {
            for (entityId in state.getBattlefield(playerId)) {
                val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
                val def = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                for (ability in def.script.staticAbilities) {
                    if (ability is GrantFlashToSpellType) {
                        // If controllerOnly, only the permanent's controller benefits
                        if (ability.controllerOnly && playerId != spellOwner) continue
                        // "The first [type] spell you cast each turn" — the grant covers only one
                        // spell per turn (Radagast of Rhosgobel).
                        if (!FlashTypeGrants.nthGateAllows(state, spellOwner, ability, predicateEvaluator)) continue
                        if (predicateEvaluator.matches(state, state.projectedState, spellCardId, ability.filter, context)) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    /**
     * Choose which permanent type to consume for graveyard casting.
     * If a card has multiple permanent types, pick the first type with available permission.
     */
    fun choosePermanentTypeForGraveyardPermission(
        state: GameState,
        playerId: EntityId,
        cardComponent: CardComponent
    ): String? {
        val permanentTypes = cardComponent.typeLine.cardTypes.filter { it.isPermanent }
        for (type in permanentTypes) {
            if (findGraveyardPlayPermissionSource(state, playerId, type.name) != null) {
                return type.name
            }
        }
        return null
    }

    /**
     * Record that a Muldrotha-like permanent's graveyard play permission was used for a type.
     */
    fun recordGraveyardPlayPermissionUsage(
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

    // --- Private helpers ---

    private fun hasCastFromTopOfLibraryPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return false
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.script.staticAbilities) {
                // Honor a conditional gate (e.g. The Lunar Whale's "as long as it attacked this
                // turn") against the granting permanent before allowing the cast from top.
                val unwrapped = if (ability is ConditionalStaticAbility) {
                    val ctx = EffectContext(sourceId = entityId, controllerId = playerId)
                    if (!conditionEvaluator.evaluate(state, ability.condition, ctx)) continue
                    ability.ability
                } else ability
                if (unwrapped is CastSpellTypesFromTopOfLibrary) {
                    val uses = state.getEntity(entityId)
                        ?.get<CastFromTopOfLibraryUsesThisTurnComponent>()?.uses ?: 0
                    val maxCasts = unwrapped.maxCastsPerTurn
                    val hasUse = maxCasts == null || uses < maxCasts
                    if (hasUse && matchesCardFilter(cardComponent, unwrapped.filter)) return true
                }
                if (unwrapped is PlayLandsAndCastFilteredFromTopOfLibrary) {
                    // A null spellFilter is a lands-only permission: nothing is castable.
                    val spellFilter = unwrapped.spellFilter
                    if (spellFilter != null && matchesCardFilter(cardComponent, spellFilter)) return true
                }
            }
        }
        return false
    }

    /**
     * Returns the limited top-of-library permission source that should consume a use for this cast.
     * An unlimited matching source wins without consuming a limited source. Otherwise each source
     * has its own allowance, matching the identity of the granting permanent.
     */
    fun findLimitedTopLibraryCastSourceToConsume(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): EntityId? {
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return null
        // A card-specific MayPlayPermission (for example, a reveal-then-cast effect whose card
        // remains in the library) can authorize the cast independently of a battlefield source.
        if (state.hasMayPlayFor(cardId, playerId, conditionEvaluator, cardRegistry)) return null
        var limitedSource: EntityId? = null
        for (entityId in state.getBattlefield(playerId)) {
            val sourceCard = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(sourceCard.cardDefinitionId) ?: continue
            for (ability in cardDef.script.staticAbilities) {
                val unwrapped = if (ability is ConditionalStaticAbility) {
                    val ctx = EffectContext(sourceId = entityId, controllerId = playerId)
                    if (!conditionEvaluator.evaluate(state, ability.condition, ctx)) continue
                    ability.ability
                } else ability
                if (unwrapped is PlayFromTopOfLibrary) return null
                if (unwrapped is PlayLandsAndCastFilteredFromTopOfLibrary &&
                    unwrapped.spellFilter?.let { matchesCardFilter(cardComponent, it) } == true
                ) return null
                if (unwrapped !is CastSpellTypesFromTopOfLibrary ||
                    !matchesCardFilter(cardComponent, unwrapped.filter)
                ) continue
                val maxCasts = unwrapped.maxCastsPerTurn ?: return null
                val uses = state.getEntity(entityId)
                    ?.get<CastFromTopOfLibraryUsesThisTurnComponent>()?.uses ?: 0
                if (uses < maxCasts && limitedSource == null) limitedSource = entityId
            }
        }
        return limitedSource
    }

    private fun hasPlayFromTopOfLibrary(state: GameState, playerId: EntityId): Boolean {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is PlayFromTopOfLibrary }) {
                return true
            }
        }
        return playFromTopAlternativeCost(state, playerId) != null
    }

    /**
     * The effective [com.wingedsheep.sdk.scripting.PlayFromTopWithAlternativeCost] for [playerId]
     * (Gwenom) — printed on a permanent they control or granted durationally — or null. Mirrors the
     * printed-or-granted scan in `CastPermissionUtils.playFromTopAlternativeCost`.
     */
    private fun playFromTopAlternativeCost(
        state: GameState,
        playerId: EntityId
    ): com.wingedsheep.sdk.scripting.PlayFromTopWithAlternativeCost? {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            cardDef.script.staticAbilities
                .firstOrNull { it is com.wingedsheep.sdk.scripting.PlayFromTopWithAlternativeCost }
                ?.let { return it as com.wingedsheep.sdk.scripting.PlayFromTopWithAlternativeCost }
        }
        for (grant in state.grantedStaticAbilities) {
            val ability = grant.ability
            if (ability !is com.wingedsheep.sdk.scripting.PlayFromTopWithAlternativeCost) continue
            if (state.getEntity(grant.entityId)?.get<ControllerComponent>()?.playerId != playerId) continue
            return ability
        }
        return null
    }

    /**
     * The [com.wingedsheep.sdk.scripting.PlayFromTopWithAlternativeCost] covering [cardId] when it is
     * the top card of [playerId]'s library and the grant's filter matches — else null. Used by
     * `CastSpellHandler` to inject the grant's additional cost (Gwenom's pay-life) into a
     * top-of-library cast.
     */
    fun topOfLibraryAlternativeGrant(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): com.wingedsheep.sdk.scripting.PlayFromTopWithAlternativeCost? {
        if (state.getLibrary(playerId).firstOrNull() != cardId) return null
        val grant = playFromTopAlternativeCost(state, playerId) ?: return null
        val filter = grant.filter ?: return grant
        return if (predicateEvaluator.matches(state, state.projectedState, cardId, filter, PredicateContext(controllerId = playerId))) grant else null
    }

    private fun hasLinkedExileCastPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean = findLinkedExileGranter(state, playerId, cardId) != null

    /**
     * Locate the battlefield permanent (controlled by [playerId]) whose
     * [GrantMayCastFromLinkedExile] ability currently permits casting [cardId] from exile,
     * honoring the ability's timing restriction and card filter. Returns null if no such
     * granter exists.
     */
    fun findLinkedExileGranter(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): GrantMayCastFromLinkedExile? = findLinkedExileGranterEntry(state, playerId, cardId)?.ability

    /**
     * Like [findLinkedExileGranter] but also returns the granter permanent's [EntityId].
     * Callers that need to mark the granter (e.g. for once-per-turn tracking on a
     * successful cast) use this overload.
     */
    fun findLinkedExileGranterEntry(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): LinkedExileGranter? {
        val cardContainer = state.getEntity(cardId) ?: return null
        val cardComponent = cardContainer.get<CardComponent>() ?: return null

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val controller = container.get<ControllerComponent>()?.playerId ?: continue
            if (controller != playerId) continue

            val linked = container.get<LinkedExileComponent>() ?: continue
            if (cardId !in linked.exiledIds) continue

            val entityCardComponent = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(entityCardComponent.cardDefinitionId) ?: continue
            val grantAbility = cardDef.script.staticAbilities
                .filterIsInstance<GrantMayCastFromLinkedExile>()
                .firstOrNull() ?: continue

            if (grantAbility.duringYourTurnOnly && !state.isActiveTurnFor(playerId)) continue

            if (grantAbility.ownedByYou && cardComponent.ownerId != playerId) continue

            if (grantAbility.oncePerTurn &&
                container.get<MayCastFromLinkedExileUsedThisTurnComponent>() != null
            ) continue

            if (grantAbility.exiledThisTurnOnly) {
                val turn = cardContainer.get<ExileEntryTurnComponent>()?.turnNumber
                if (turn == null || turn != state.turnNumber) continue
            }

            val maxManaValue = grantAbility.maxManaValue
            if (maxManaValue != null) {
                val cap = evaluateMaxManaValue(state, entityId, playerId, maxManaValue)
                if (cardComponent.manaCost.cmc > cap) continue
            }

            if (matchesCardFilter(cardComponent, grantAbility.filter)) {
                return LinkedExileGranter(entityId, grantAbility)
            }
        }
        return null
    }

    private fun evaluateMaxManaValue(
        state: GameState,
        granterId: EntityId,
        controllerId: EntityId,
        amount: com.wingedsheep.sdk.scripting.values.DynamicAmount
    ): Int {
        val context = EffectContext(
            sourceId = granterId,
            controllerId = controllerId,
        )
        return DynamicAmountEvaluator().evaluate(state, amount, context)
    }

    /** Pair returned by [findLinkedExileGranterEntry] — the granter permanent and its ability. */
    data class LinkedExileGranter(
        val granterId: EntityId,
        val ability: GrantMayCastFromLinkedExile
    )

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

    companion object {
        fun matchesCardFilter(card: CardComponent, filter: GameObjectFilter): Boolean {
            for (predicate in filter.cardPredicates) {
                if (!matchesCardPredicate(card, predicate)) return false
            }
            return true
        }

        private fun matchesCardPredicate(card: CardComponent, predicate: CardPredicate): Boolean {
            val cmc = card.manaCost.cmc
            val power = card.baseStats?.basePower
            val toughness = card.baseStats?.baseToughness
            return when (predicate) {
                // --- Card types ---
                is CardPredicate.IsInstant -> card.typeLine.isInstant
                is CardPredicate.IsSorcery -> card.typeLine.isSorcery
                is CardPredicate.IsCreature -> card.typeLine.isCreature
                is CardPredicate.IsNoncreature -> !card.typeLine.isCreature
                is CardPredicate.IsEnchantment -> card.typeLine.isEnchantment
                is CardPredicate.IsNonenchantment -> !card.typeLine.isEnchantment
                is CardPredicate.IsArtifact -> card.typeLine.isArtifact
                is CardPredicate.IsNonartifact -> !card.typeLine.isArtifact
                is CardPredicate.IsLand -> card.typeLine.isLand
                is CardPredicate.IsNonland -> !card.typeLine.isLand
                is CardPredicate.IsPlaneswalker -> card.isPlaneswalker
                is CardPredicate.IsPermanent -> card.typeLine.isPermanent
                is CardPredicate.IsBasicLand -> card.typeLine.isBasicLand
                is CardPredicate.HasAdventure -> card.hasAdventure
                is CardPredicate.HasNoAbilities -> card.oracleText.isBlank()
                // --- Supertypes ---
                is CardPredicate.IsLegendary -> card.typeLine.isLegendary
                is CardPredicate.IsNonlegendary -> !card.typeLine.isLegendary
                // --- Colors ---
                is CardPredicate.HasColor -> predicate.color in card.colors
                is CardPredicate.NotColor -> predicate.color !in card.colors
                is CardPredicate.IsColorless -> card.colors.isEmpty()
                is CardPredicate.IsColored -> card.colors.isNotEmpty()
                is CardPredicate.IsMulticolored -> card.colors.size >= 2
                is CardPredicate.IsMonocolored -> card.colors.size == 1
                // --- Subtypes ---
                is CardPredicate.HasSubtype -> card.typeLine.hasSubtype(predicate.subtype)
                is CardPredicate.HasAnyOfSubtypes ->
                    predicate.subtypes.any { card.typeLine.hasSubtype(it) }
                is CardPredicate.NotSubtype -> !card.typeLine.hasSubtype(predicate.subtype)
                is CardPredicate.HasBasicLandType ->
                    card.typeLine.hasSubtype(com.wingedsheep.sdk.core.Subtype(predicate.landType))
                // --- Name / origin ---
                is CardPredicate.NameEquals -> card.name == predicate.name
                is CardPredicate.OriginallyPrintedInSet ->
                    card.originalSetCode?.equals(predicate.setCode, ignoreCase = true) == true
                // --- Keywords ---
                is CardPredicate.HasKeyword -> predicate.keyword in card.baseKeywords
                is CardPredicate.NotKeyword -> predicate.keyword !in card.baseKeywords
                // --- Mana value ---
                is CardPredicate.ManaValueEquals -> cmc == predicate.value
                is CardPredicate.ManaValueAtMost -> cmc <= predicate.max
                is CardPredicate.ManaValueAtLeast -> cmc >= predicate.min
                is CardPredicate.ManaValueIsEven -> cmc % 2 == 0
                is CardPredicate.ManaValueIsOdd -> cmc % 2 != 0
                is CardPredicate.HasXInManaCost -> card.manaCost.hasX
                // --- Power / toughness (null base P/T — e.g. */noncreature — never matches) ---
                is CardPredicate.PowerEquals -> power == predicate.value
                is CardPredicate.PowerAtMost -> power != null && power <= predicate.max
                is CardPredicate.PowerAtLeast -> power != null && power >= predicate.min
                is CardPredicate.ToughnessEquals -> toughness == predicate.value
                is CardPredicate.ToughnessAtMost -> toughness != null && toughness <= predicate.max
                is CardPredicate.ToughnessAtLeast -> toughness != null && toughness >= predicate.min
                is CardPredicate.PowerOrToughnessAtLeast ->
                    (power != null && power >= predicate.min) || (toughness != null && toughness >= predicate.min)
                is CardPredicate.PowerOrToughnessAtMost ->
                    (power != null && power <= predicate.max) || (toughness != null && toughness <= predicate.max)
                is CardPredicate.TotalPowerAndToughnessAtMost ->
                    power != null && toughness != null && power + toughness <= predicate.max
                is CardPredicate.ToughnessGreaterThanPower ->
                    power != null && toughness != null && toughness > power
                // --- Intrinsic activated abilities (precomputed flags) ---
                is CardPredicate.HasActivatedAbility -> card.hasActivatedAbility
                is CardPredicate.HasNonManaActivatedAbility -> card.hasNonManaActivatedAbility
                // --- Combinators ---
                is CardPredicate.Or -> predicate.predicates.any { matchesCardPredicate(card, it) }
                is CardPredicate.And -> predicate.predicates.all { matchesCardPredicate(card, it) }
                is CardPredicate.Not -> !matchesCardPredicate(card, predicate.predicate)
                // Predicates that can't be judged from a card's static characteristics alone —
                // they need runtime/interaction context (a chosen value, another entity, X, a
                // pipeline variable/stored group, the recipient/source of an effect), or describe
                // a stack ability rather than a castable card. No cast-from-zone filter uses them,
                // and matching a condition we can't verify would silently widen the grant, so they
                // fail closed. This `when` is exhaustive: adding a CardPredicate forces a decision
                // here rather than leaking through a permissive `else`.
                is CardPredicate.IsToken,
                is CardPredicate.IsNontoken,
                is CardPredicate.HasChosenColor,
                is CardPredicate.HasChosenSubtype,
                is CardPredicate.SharesChosenColorWithSource,
                is CardPredicate.NameEqualsChosen,
                is CardPredicate.NameEqualsChosenComponent,
                is CardPredicate.CardTypeEqualsChosenComponent,
                is CardPredicate.NameNotSharedWithControlledRoom,
                is CardPredicate.NameNotSharedWithControlledToken,
                is CardPredicate.NameNotSharedWithAnotherControlledPermanent,
                is CardPredicate.ManaValueEqualsX,
                is CardPredicate.ManaValueAtMostX,
                is CardPredicate.ManaValueAtMostEntity,
                is CardPredicate.ManaValueAtMostEntityManaSpent,
                is CardPredicate.ManaValueAtMostColorsSpent,
                is CardPredicate.ManaValueAtMostDynamic,
                is CardPredicate.ManaValueEqualsDynamic,
                is CardPredicate.PowerEqualsDynamic,
                is CardPredicate.ToughnessEqualsDynamic,
                is CardPredicate.PowerEqualsX,
                is CardPredicate.PowerAtLeastX,
                is CardPredicate.ToughnessAtMostX,
                is CardPredicate.PowerAtMostEntity,
                is CardPredicate.PowerGreaterThanEntity,
                is CardPredicate.PowerLessThanEntity,
                // A card in a zone has no projected pump, so its power never exceeds its own
                // base power — never greater (mirrors CostCalculator's static treatment).
                is CardPredicate.PowerGreaterThanBase,
                is CardPredicate.HasSubtypeFromVariable,
                is CardPredicate.HasSubtypeInStoredList,
                is CardPredicate.HasSubtypeInEachStoredGroup,
                is CardPredicate.NotOfSourceChosenType,
                is CardPredicate.SharesCreatureTypeWithSource,
                is CardPredicate.SharesCreatureTypeWithTriggeringEntity,
                is CardPredicate.SharesCreatureTypeWith,
                is CardPredicate.SharesColorWith,
                is CardPredicate.SharesColorWithRecipient,
                is CardPredicate.SharesColorWithPermanentYouControl,
                is CardPredicate.SharesNameWithPermanentYouControl,
                is CardPredicate.DoesNotShareCreatureTypeWithPermanentYouControl,
                is CardPredicate.DoesNotShareLandTypeWithPermanentYouControl,
                is CardPredicate.TargetsMatching,
                is CardPredicate.AbilitySourceMatches,
                is CardPredicate.IsActivatedOrTriggeredAbility,
                is CardPredicate.IsTriggeredAbility,
                is CardPredicate.IsActivatedAbility -> false
            }
        }
    }
}
