package com.wingedsheep.engine.handlers.actions.spell

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.mechanics.HarmonizeGrants
import com.wingedsheep.engine.mechanics.WarpGrants
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.ExileEntryTurnComponent
import com.wingedsheep.engine.state.components.battlefield.GraveyardPlayPermissionUsedComponent
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
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
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
        if (state.hasMayPlayFor(cardId, playerId, conditionEvaluator)) return true

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
    ): Boolean {
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return false
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return false
        val mayCastAbility = cardDef.script.staticAbilities
            .filterIsInstance<MayCastSelfFromZones>()
            .firstOrNull() ?: return false

        // Find what zone the card is in for this player
        for (zone in mayCastAbility.zones) {
            val zoneKey = ZoneKey(playerId, zone)
            if (cardId in state.getZone(zoneKey)) return true
        }
        return false
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
        if (state.activePlayerId != playerId) return false

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
    ): Boolean {
        if (cardId !in state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))) return false
        for (permId in state.getBattlefield(playerId)) {
            val permCard = state.getEntity(permId)?.get<CardComponent>() ?: continue
            val permDef = cardRegistry.getCard(permCard.cardDefinitionId) ?: continue
            for (sa in permDef.script.staticAbilities) {
                if (sa !is MayCastFromGraveyard) continue
                if (sa.duringYourTurnOnly && state.activePlayerId != playerId) continue
                if (predicateEvaluator.matches(
                        state, state.projectedState, cardId, sa.filter,
                        PredicateContext(controllerId = playerId)
                    )
                ) return true
            }
        }
        return false
    }

    /**
     * Check if a card in the graveyard has a Flashback keyword ability,
     * allowing it to be cast from the graveyard for its flashback cost.
     */
    fun hasFlashbackPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)
        if (cardId !in state.getZone(graveyardZone)) return false
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return false
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return false
        return cardDef.keywordAbilities.any { it is KeywordAbility.Flashback }
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
     * Get the flashback cost for a card, or null if it doesn't have flashback.
     */
    fun getFlashbackCost(cardId: EntityId, state: GameState): com.wingedsheep.sdk.core.ManaCost? {
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return null
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return null
        val flashback = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Flashback>().firstOrNull()
        return flashback?.cost
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
        return granter?.withoutPayingManaCost == true
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
                if (ability is CastSpellTypesFromTopOfLibrary) {
                    if (matchesCardFilter(cardComponent, ability.filter)) return true
                }
                if (ability is PlayLandsAndCastFilteredFromTopOfLibrary) {
                    if (matchesCardFilter(cardComponent, ability.spellFilter)) return true
                }
            }
        }
        return false
    }

    private fun hasPlayFromTopOfLibrary(state: GameState, playerId: EntityId): Boolean {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is PlayFromTopOfLibrary }) {
                return true
            }
        }
        return false
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

            if (grantAbility.duringYourTurnOnly && state.activePlayerId != playerId) continue

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
            return when (predicate) {
                is CardPredicate.IsInstant -> card.typeLine.isInstant
                is CardPredicate.IsSorcery -> card.typeLine.isSorcery
                is CardPredicate.IsCreature -> card.typeLine.isCreature
                is CardPredicate.IsEnchantment -> card.typeLine.isEnchantment
                is CardPredicate.IsArtifact -> card.typeLine.isArtifact
                is CardPredicate.IsLand -> card.typeLine.isLand
                is CardPredicate.ManaValueAtLeast -> card.manaCost.cmc >= predicate.min
                is CardPredicate.ManaValueAtMost -> card.manaCost.cmc <= predicate.max
                is CardPredicate.ManaValueEquals -> card.manaCost.cmc == predicate.value
                is CardPredicate.Or -> predicate.predicates.any { matchesCardPredicate(card, it) }
                is CardPredicate.And -> predicate.predicates.all { matchesCardPredicate(card, it) }
                is CardPredicate.Not -> !matchesCardPredicate(card, predicate.predicate)
                else -> true // Conservative: allow unknown predicates
            }
        }
    }
}
