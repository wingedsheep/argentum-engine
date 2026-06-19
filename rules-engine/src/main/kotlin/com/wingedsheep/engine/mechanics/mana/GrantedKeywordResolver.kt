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
     * The effective numeric parameter for a parameterized keyword on [cardDef] when cast by
     * [playerId] — e.g. the Casualty power threshold. Reads the printed [KeywordAbility]
     * parameter first, then any granting source's [GrantKeywordToOwnSpells.keywordParameter].
     * Returns null when the spell does not have the keyword.
     */
    fun casualtyThreshold(
        state: GameState,
        playerId: EntityId,
        cardDef: CardDefinition
    ): Int? {
        val printed = cardDef.keywordAbilities
            .filterIsInstance<com.wingedsheep.sdk.scripting.KeywordAbility.Casualty>()
            .firstOrNull()
        if (printed != null) return printed.threshold
        val grantSource = findGrant(state, playerId, cardDef, Keyword.CASUALTY) ?: return null
        val sourceDef = state.getEntity(grantSource)?.get<CardComponent>()
            ?.let { cardRegistry.getCard(it.cardDefinitionId) } ?: return null
        return sourceDef.script.staticAbilities
            .filterIsInstance<GrantKeywordToOwnSpells>()
            .firstOrNull { it.keyword == Keyword.CASUALTY && matchesSpellFilter(it.spellFilter, cardDef) }
            ?.keywordParameter
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
     * Count how many battlefield permanents controlled by [playerId] grant [keyword] to [cardDef]
     * via [GrantKeywordToOwnSpells]. Unlike [findGrant], this does not short-circuit — each grant
     * counts as a separate instance, which matters for "each instance triggers separately"
     * keywords like STORM (CR 702.40b: e.g. two Prismaris grant two storm triggers).
     */
    fun countGrants(
        state: GameState,
        playerId: EntityId,
        cardDef: CardDefinition,
        keyword: Keyword
    ): Int {
        var count = 0
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
                    count++
                }
            }
        }
        return count
    }

    /**
     * Match a spell's [cardDef] against a [GameObjectFilter] using the card's printed
     * characteristics. Only a narrow set of predicates is supported because spells being cast
     * don't yet have battlefield state — the caller knows the card, not an entity.
     */
    private fun matchesSpellFilter(filter: GameObjectFilter, cardDef: CardDefinition): Boolean {
        // GameObjectFilter.Any / empty predicates match everything.
        if (filter.cardPredicates.isEmpty() && filter.controllerPredicate == null) return true
        return filter.cardPredicates.all { matchesPredicate(it, cardDef) }
    }

    /**
     * Evaluate a single [CardPredicate] against a [CardDefinition]. Recurses through the
     * boolean combinators ([CardPredicate.And]/[Or]/[Not]) so combined-type filters such as
     * [GameObjectFilter.InstantOrSorcery] (an `Or` of `IsInstant`/`IsSorcery`) resolve, which
     * is what "instant and sorcery spells you cast have storm" needs.
     */
    private fun matchesPredicate(predicate: CardPredicate, cardDef: CardDefinition): Boolean =
        when (predicate) {
            is CardPredicate.And -> predicate.predicates.all { matchesPredicate(it, cardDef) }
            is CardPredicate.Or -> predicate.predicates.any { matchesPredicate(it, cardDef) }
            is CardPredicate.Not -> !matchesPredicate(predicate.predicate, cardDef)
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
            CardPredicate.IsNonartifact -> !cardDef.typeLine.isArtifact
            CardPredicate.IsToken, CardPredicate.IsNontoken -> true
            is CardPredicate.HasSubtype -> cardDef.typeLine.subtypes.any { it == predicate.subtype }
            is CardPredicate.HasAnyOfSubtypes -> cardDef.typeLine.subtypes.any { predicate.subtypes.contains(it) }
            is CardPredicate.HasColor -> cardDef.colors.contains(predicate.color)
            CardPredicate.IsColorless -> cardDef.colors.isEmpty()
            // Fail closed: an unhandled predicate cannot be evaluated against a
            // CardDefinition alone, so we conservatively refuse to grant the keyword
            // rather than silently match every spell. Add explicit handling here when
            // a new spell-filter predicate is needed (e.g. ManaValue, NameEquals).
            else -> false
        }
}
