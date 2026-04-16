package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeywordToOwnSpells
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Resolves whether a spell being cast effectively has a given keyword, either because it is
 * printed on the card or because a permanent the caster controls grants it via
 * [GrantKeywordToOwnSpells] (e.g., Eirdu: "Creature spells you cast have convoke.").
 *
 * Kept as a small utility separate from [AlternativePaymentHandler] so the lookup can be shared
 * across the enumerator, the cost utils, and the payment handler without cycles.
 */
class GrantedKeywordResolver(
    private val cardRegistry: CardRegistry
) {
    /**
     * Does [cardDef], if cast by [playerId] in [state], effectively have [keyword]?
     *
     * Returns true if either the card prints the keyword or at least one battlefield permanent
     * controlled by [playerId] has a [GrantKeywordToOwnSpells] static ability whose
     * `spellFilter` matches [cardDef].
     */
    fun hasKeyword(
        state: GameState,
        playerId: EntityId,
        cardDef: CardDefinition,
        keyword: Keyword
    ): Boolean {
        if (cardDef.keywords.contains(keyword)) return true
        return findGrant(state, playerId, cardDef, keyword) != null
    }

    /**
     * Find the first battlefield source that grants [keyword] to [cardDef] when cast by [playerId].
     * Returns null if no grant applies (or none exists).
     */
    fun findGrant(
        state: GameState,
        playerId: EntityId,
        cardDef: CardDefinition,
        keyword: Keyword
    ): EntityId? {
        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val controllerId = container.get<ControllerComponent>()?.playerId ?: continue
            if (controllerId != playerId) continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val sourceDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
            for (ability in sourceDef.script.staticAbilities) {
                if (ability is GrantKeywordToOwnSpells &&
                    ability.keyword == keyword &&
                    matchesSpellFilter(ability.spellFilter, cardDef)
                ) {
                    return permanentId
                }
            }
        }
        return null
    }

    /**
     * Match a spell's [cardDef] against a [GameObjectFilter] using the card's printed
     * characteristics. Only a narrow set of predicates is supported because spells being cast
     * don't yet have battlefield state — the caller knows the card, not an entity.
     */
    private fun matchesSpellFilter(filter: GameObjectFilter, cardDef: CardDefinition): Boolean {
        // GameObjectFilter.Any / empty predicates match everything.
        if (filter.cardPredicates.isEmpty() && filter.controllerPredicate == null) return true
        for (predicate in filter.cardPredicates) {
            val matches = when (predicate) {
                CardPredicate.IsCreature -> cardDef.typeLine.isCreature
                CardPredicate.IsNoncreature -> !cardDef.typeLine.isCreature
                CardPredicate.IsArtifact -> cardDef.typeLine.isArtifact
                CardPredicate.IsEnchantment -> cardDef.typeLine.isEnchantment
                CardPredicate.IsInstant -> cardDef.typeLine.isInstant
                CardPredicate.IsSorcery -> cardDef.typeLine.isSorcery
                CardPredicate.IsLand -> cardDef.typeLine.isLand
                CardPredicate.IsNonland -> !cardDef.typeLine.isLand
                CardPredicate.IsLegendary -> cardDef.typeLine.isLegendary
                CardPredicate.IsNonlegendary -> !cardDef.typeLine.isLegendary
                CardPredicate.IsPermanent -> cardDef.typeLine.isPermanent
                CardPredicate.IsNonenchantment -> !cardDef.typeLine.isEnchantment
                CardPredicate.IsToken, CardPredicate.IsNontoken -> true
                is CardPredicate.HasSubtype -> cardDef.typeLine.subtypes.any { it == predicate.subtype }
                is CardPredicate.HasAnyOfSubtypes -> cardDef.typeLine.subtypes.any { predicate.subtypes.contains(it) }
                is CardPredicate.HasColor -> cardDef.colors.contains(predicate.color)
                CardPredicate.IsColorless -> cardDef.colors.isEmpty()
                else -> true
            }
            if (!matches) return false
        }
        return true
    }
}
