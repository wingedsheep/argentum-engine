package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CharacteristicValue
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Whether a printed card, sitting in a deck list, would match a [GameObjectFilter] once it is cast
 * or in play: the deck-building reading of the "noncreature spell", "artifact you control" or
 * "creature with power 4 or greater" a payoff asks for ([CardAsks]).
 *
 * Three-valued, because a deck list can't answer every predicate: `null` means the filter names
 * something only a game knows (a chosen colour, another object's power, a target). A caller drops
 * such a filter rather than reading it as matching nothing, which would call its payoff unfed in
 * every deck. State predicates (tapped, attacking, entered this turn) are game state any card can
 * be in, so they don't narrow the match.
 *
 * It follows [com.wingedsheep.sdk.model.CompanionEligibility.matchesPrinted], which fails closed for
 * a deck-legality check; this one has to tell "no" from "can't tell".
 */
internal object PrintedFilter {

    fun matches(card: CardDefinition, filter: GameObjectFilter): Boolean? {
        val base = all(filter.cardPredicates.map { matches(card, it) })
        if (base == false || filter.anyOf.isEmpty()) return base
        return and(base, any(filter.anyOf.map { matches(card, it) }))
    }

    /**
     * Whether [filter] asks for something narrower than a plain creature, spell or land: it rejects at
     * least one vanilla creature of some colour and size and, unless it filters [spells] (which are
     * never lands), every basic land. "Spell", "permanent", "creature" and landfall's "land you control"
     * don't — every deck feeds those, and [SynergyAxis.CREATURES_ENTER] already reads the creature
     * case. Null when the filter can't be read off a printed card at all.
     */
    fun isNarrow(filter: GameObjectFilter, spells: Boolean): Boolean? {
        if (!spells) for (land in BASIC_LANDS) when (matches(land, filter)) {
            null -> return null
            true -> return false
            false -> Unit
        }
        var narrow = false
        for (probe in PROBES) {
            when (matches(probe, filter)) {
                null -> return null
                false -> narrow = true
                true -> Unit
            }
        }
        return narrow
    }

    private fun matches(card: CardDefinition, predicate: CardPredicate): Boolean? {
        val typeLine = card.typeLine
        val manaCost = if (card.isSplit && card.cardFaces.isNotEmpty()) {
            ManaCost(card.cardFaces.flatMap { it.manaCost.symbols })
        } else {
            card.manaCost
        }
        val manaValue = manaCost.cmc
        return when (predicate) {
            is CardPredicate.And -> all(predicate.predicates.map { matches(card, it) })
            is CardPredicate.Or -> any(predicate.predicates.map { matches(card, it) })
            is CardPredicate.Not -> matches(card, predicate.predicate)?.not()
            CardPredicate.IsCreature -> typeLine.isCreature
            CardPredicate.IsLand -> typeLine.isLand
            CardPredicate.IsArtifact -> typeLine.isArtifact
            CardPredicate.IsEnchantment -> typeLine.isEnchantment
            CardPredicate.IsPlaneswalker -> CardType.PLANESWALKER in typeLine.cardTypes
            CardPredicate.IsBattle -> typeLine.isBattle
            CardPredicate.IsInstant -> typeLine.isInstant
            CardPredicate.IsSorcery -> typeLine.isSorcery
            CardPredicate.IsPermanent -> typeLine.isPermanent
            CardPredicate.IsNonland -> !typeLine.isLand
            CardPredicate.IsNoncreature -> !typeLine.isCreature
            CardPredicate.IsNonenchantment -> !typeLine.isEnchantment
            CardPredicate.IsNonartifact -> !typeLine.isArtifact
            CardPredicate.IsBasicLand -> typeLine.isBasicLand
            CardPredicate.IsLegendary -> typeLine.isLegendary
            CardPredicate.IsNonlegendary -> !typeLine.isLegendary
            CardPredicate.IsSnow -> typeLine.isSnow
            // A card in a deck is never a token; token makers feed through their own axis.
            CardPredicate.IsToken -> null
            CardPredicate.IsNontoken -> true
            is CardPredicate.HasColor -> predicate.color in card.colors
            is CardPredicate.NotColor -> predicate.color !in card.colors
            CardPredicate.IsColorless -> card.colors.isEmpty()
            CardPredicate.IsColored -> card.colors.isNotEmpty()
            CardPredicate.IsMulticolored -> card.colors.size > 1
            CardPredicate.IsMonocolored -> card.colors.size == 1
            is CardPredicate.HasExactlyColors -> card.colors.size == predicate.count
            is CardPredicate.HasSubtype -> typeLine.hasSubtype(predicate.subtype)
            is CardPredicate.NotSubtype -> !typeLine.hasSubtype(predicate.subtype)
            is CardPredicate.HasAnyOfSubtypes -> predicate.subtypes.any { typeLine.hasSubtype(it) }
            is CardPredicate.NameEquals -> card.name == predicate.name
            is CardPredicate.HasKeyword -> predicate.keyword in card.keywords
            is CardPredicate.NotKeyword -> predicate.keyword !in card.keywords
            is CardPredicate.ManaValueEquals -> manaValue == predicate.value
            is CardPredicate.ManaValueAtMost -> manaValue <= predicate.max
            is CardPredicate.ManaValueAtLeast -> manaValue >= predicate.min
            CardPredicate.ManaValueIsEven -> manaValue % 2 == 0
            CardPredicate.ManaValueIsOdd -> manaValue % 2 == 1
            CardPredicate.HasXInManaCost -> manaCost.hasX
            CardPredicate.HasCycling -> card.hasCycling
            is CardPredicate.PowerEquals -> stat(card, power = true) { it == predicate.value }
            is CardPredicate.BasePowerEquals -> stat(card, power = true) { it == predicate.value }
            is CardPredicate.PowerAtMost -> stat(card, power = true) { it <= predicate.max }
            is CardPredicate.PowerAtLeast -> stat(card, power = true) { it >= predicate.min }
            is CardPredicate.ToughnessEquals -> stat(card, power = false) { it == predicate.value }
            is CardPredicate.BaseToughnessEquals -> stat(card, power = false) { it == predicate.value }
            is CardPredicate.ToughnessAtMost -> stat(card, power = false) { it <= predicate.max }
            is CardPredicate.ToughnessAtLeast -> stat(card, power = false) { it >= predicate.min }
            is CardPredicate.PowerOrToughnessAtLeast -> or(
                stat(card, power = true) { it >= predicate.min },
                stat(card, power = false) { it >= predicate.min },
            )
            is CardPredicate.PowerOrToughnessAtMost -> or(
                stat(card, power = true) { it <= predicate.max },
                stat(card, power = false) { it <= predicate.max },
            )
            else -> null
        }
    }

    /**
     * A printed power or toughness test. A noncreature card has none, so it fails; a `*` can't be
     * read off the card, so it is unknown.
     */
    private fun stat(card: CardDefinition, power: Boolean, test: (Int) -> Boolean): Boolean? {
        val stats = card.creatureStats ?: return false
        val value = if (power) stats.power else stats.toughness
        return (value as? CharacteristicValue.Fixed)?.let { test(it.value) }
    }

    private fun all(values: List<Boolean?>): Boolean? = values.fold(true as Boolean?, ::and)

    private fun any(values: List<Boolean?>): Boolean? = values.fold(false as Boolean?, ::or)

    private fun and(a: Boolean?, b: Boolean?): Boolean? = when {
        a == false || b == false -> false
        a == null || b == null -> null
        else -> true
    }

    private fun or(a: Boolean?, b: Boolean?): Boolean? = when {
        a == true || b == true -> true
        a == null || b == null -> null
        else -> false
    }

    private val BASIC_LANDS: List<CardDefinition> =
        Subtype.ALL_BASIC_LAND_TYPES.map { CardDefinition.basicLand("Probe $it", Subtype(it)) }

    /** A vanilla creature of each colour (and colourless) at three sizes: what "plain creature" means. */
    private val PROBES: List<CardDefinition> = buildList {
        for (pip in listOf("{W}", "{U}", "{B}", "{R}", "{G}", "")) for (size in listOf(1, 3, 6)) {
            val generic = if (pip.isEmpty()) size else size - 1
            val cost = (if (generic > 0) "{$generic}" else "") + pip
            add(CardDefinition.creature("Probe $pip$size", ManaCost.parse(cost), emptySet(), size, size))
        }
    }
}
