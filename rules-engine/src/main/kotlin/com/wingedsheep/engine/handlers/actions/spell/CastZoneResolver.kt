package com.wingedsheep.engine.handlers.actions.spell

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.GraveyardPlayPermissionUsedComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.MayPlayFromExileComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.identity.WarpExiledComponent
import com.wingedsheep.engine.state.components.player.MayCastCreaturesFromGraveyardWithForageComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.KeywordAbility
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
     * Check if a card is in exile or a graveyard and has `MayPlayFromExileComponent`
     * granting the player permission to play it. Checks all players' exile zones
     * because cards like Villainous Wealth exile from an opponent's library (cards
     * remain in their owner's exile zone but are castable by the spell's controller).
     * Graveyard coverage handles free-cast grants that leave the card in the
     * graveyard (e.g. Malcolm, Alluring Scoundrel).
     *
     * Also checks for permanents with GrantMayCastFromLinkedExile static ability
     * (e.g., Rona, Disciple of Gix) that link exiled cards via LinkedExileComponent.
     */
    fun isInExileWithPlayPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val inExileOrGraveyard = state.turnOrder.any { pid ->
            cardId in state.getZone(ZoneKey(pid, Zone.EXILE)) ||
                cardId in state.getZone(ZoneKey(pid, Zone.GRAVEYARD))
        }
        if (!inExileOrGraveyard) return false

        // Check direct MayPlayFromExileComponent grant
        val component = state.getEntity(cardId)?.get<MayPlayFromExileComponent>()
        if (component?.controllerId == playerId) return true

        // Check for GrantMayCastFromLinkedExile static abilities on battlefield permanents
        return hasLinkedExileCastPermission(state, playerId, cardId)
    }

    /**
     * Check if a card has an intrinsic MayCastSelfFromZones static ability
     * that permits casting from its current zone (e.g., Squee, the Immortal).
     */
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
     * Get the flashback cost for a card, or null if it doesn't have flashback.
     */
    fun getFlashbackCost(cardId: EntityId, state: GameState): com.wingedsheep.sdk.core.ManaCost? {
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return null
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return null
        val flashback = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Flashback>().firstOrNull()
        return flashback?.cost
    }

    /**
     * Check if a card in the hand has a Warp keyword ability,
     * allowing it to be cast for its warp cost.
     */
    fun hasWarpPermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val handZone = ZoneKey(playerId, Zone.HAND)
        if (cardId !in state.getZone(handZone)) return false
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return false
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return false
        return cardDef.keywordAbilities.any { it is KeywordAbility.Warp }
    }

    /**
     * Check if a card in exile has WarpExiledComponent granting
     * the player permission to re-cast it using its warp cost.
     */
    fun hasWarpFromExilePermission(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val inAnyExile = state.turnOrder.any { pid ->
            cardId in state.getZone(ZoneKey(pid, Zone.EXILE))
        }
        if (!inAnyExile) return false
        val warpExiled = state.getEntity(cardId)?.get<WarpExiledComponent>() ?: return false
        return warpExiled.controllerId == playerId
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
     */
    fun hasPlayWithoutPayingCost(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId
    ): Boolean {
        val component = state.getEntity(cardId)?.get<PlayWithoutPayingCostComponent>()
        return component?.controllerId == playerId
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
            val opponentId = state.turnOrder.firstOrNull { it != spellOwner }
            val effectContext = EffectContext(
                sourceId = spellCardId,
                controllerId = spellOwner,
                opponentId = opponentId
            )
            if (conditionEvaluator.evaluate(state, conditionalFlash, effectContext)) {
                return true
            }
        }

        // Check GrantFlashToSpellType static abilities on battlefield permanents
        val context = PredicateContext(controllerId = spellOwner)
        for (playerId in state.turnOrder) {
            for (entityId in state.getBattlefield(playerId)) {
                val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
                val def = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                for (ability in def.script.staticAbilities) {
                    if (ability is GrantFlashToSpellType) {
                        // If controllerOnly, only the permanent's controller benefits
                        if (ability.controllerOnly && playerId != spellOwner) continue
                        if (predicateEvaluator.matches(state, spellCardId, ability.filter, context)) {
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
    ): Boolean {
        val cardContainer = state.getEntity(cardId) ?: return false
        val cardComponent = cardContainer.get<CardComponent>() ?: return false

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

            if (matchesCardFilter(cardComponent, grantAbility.filter)) {
                return true
            }
        }
        return false
    }

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
