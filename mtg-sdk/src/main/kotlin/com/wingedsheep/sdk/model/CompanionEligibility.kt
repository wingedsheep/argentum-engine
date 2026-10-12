package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.CompanionCondition
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Deck-construction rules for companions (CR 702.139, 103.2b): whether a card may be revealed as a
 * deck's companion, judged against the deck's printed cards.
 *
 * Lives in the SDK beside [CommanderEligibility] for the same reason: the game-server validator,
 * and any deck builder that wants to say "this deck can't take that companion", must ask the same
 * question of the same pure card data, or a deck the builder accepts is one the server rejects.
 *
 * **Starting deck.** A companion condition reads the deck after sideboard cards are set aside and,
 * in Commander, *before* the commander is set aside (CR 702.139b) — so callers pass the commanders
 * as part of [conditionError]'s `startingDeck`. The companion itself is never part of it.
 */
object CompanionEligibility {

    /** The companion ability printed on [card], or null when it has none. */
    fun companionAbility(card: CardDefinition): KeywordAbility.CompanionAbility? =
        card.keywordAbilities.filterIsInstance<KeywordAbility.CompanionAbility>().firstOrNull()

    /**
     * [card]'s companion condition as players read it: the printed text after "Companion —" (the
     * reminder text dropped), falling back to the structured condition's description. Null when
     * the card has no companion ability.
     */
    fun conditionText(card: CardDefinition): String? {
        val ability = companionAbility(card) ?: return null
        val printed = card.oracleText.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("Companion") && '—' in it }
            ?.substringAfter('—')?.substringBefore(" (")?.trim()
        return printed?.takeIf { it.isNotEmpty() } ?: ability.condition.description
    }

    /** True when [card] has a companion ability — i.e. it *may* be revealed as a companion. */
    fun hasCompanion(card: CardDefinition): Boolean = companionAbility(card) != null

    /**
     * Why [companion] can't be revealed as the companion of a deck whose starting deck is
     * [startingDeck] (one entry per card, commanders included), or null when it can.
     *
     * @param minimumDeckSize the format's minimum deck size, for "at least N more than the minimum"
     *   (Yorion). In a format with an exact size (Commander's 100), pass that size: the deck can
     *   never exceed it, so Yorion can never be a Commander companion (ruling).
     */
    fun conditionError(
        companion: CardDefinition,
        startingDeck: List<CardDefinition>,
        minimumDeckSize: Int,
    ): String? {
        val ability = companionAbility(companion)
            ?: return "${companion.name} has no companion ability"
        val text = conditionText(companion)
        return when (val condition = ability.condition) {
            is CompanionCondition.EachCard -> {
                val offenders = startingDeck.filterNot { matchesPrinted(it, condition.filter) }
                    .map { it.name }.distinct()
                if (offenders.isEmpty()) null
                else "${companion.name}'s companion condition (\"$text\") isn't met by " +
                    offenders.take(MAX_NAMED).joinToString(", ") + more(offenders.size)
            }
            is CompanionCondition.DeckSizeAboveMinimum -> {
                val required = minimumDeckSize + condition.extra
                if (startingDeck.size >= required) null
                else "${companion.name} needs a starting deck of at least $required cards " +
                    "(${condition.extra} more than the minimum of $minimumDeckSize); this one has ${startingDeck.size}"
            }
            is CompanionCondition.ShareACardType -> {
                val considered = startingDeck.filter { matchesPrinted(it, condition.among) }
                val shared = considered
                    .map { card -> card.typeLine.cardTypes }
                    .reduceOrNull { acc, types -> acc intersect types }
                if (shared == null || shared.isNotEmpty()) null
                else "${companion.name}'s companion condition (\"$text\") isn't met — " +
                    "no single card type is common to all of them"
            }
            is CompanionCondition.DistinctNames -> {
                val repeated = startingDeck.filter { matchesPrinted(it, condition.among) }
                    .groupingBy { it.name }.eachCount()
                    .filterValues { it > 1 }.keys.toList()
                if (repeated.isEmpty()) null
                else "${companion.name}'s companion condition (\"$text\") isn't met — " +
                    "more than one copy of " + repeated.take(MAX_NAMED).joinToString(", ") + more(repeated.size)
            }
        }
    }

    /**
     * Whether the printed [card] matches [filter] — the deck-construction reading of a
     * [GameObjectFilter], where no card is in play: only printed characteristics count. Card
     * predicates are a conjunction and [GameObjectFilter.anyOf] a union, as in play. Anything that
     * needs a game (state predicates, chosen values, other objects) fails closed: an unreadable
     * condition never admits a deck.
     */
    fun matchesPrinted(card: CardDefinition, filter: GameObjectFilter): Boolean {
        if (filter.statePredicates.isNotEmpty()) return false
        if (!filter.cardPredicates.all { matchesPrinted(card, it) }) return false
        if (filter.anyOf.isNotEmpty()) return filter.anyOf.any { matchesPrinted(card, it) }
        return true
    }

    private fun matchesPrinted(card: CardDefinition, predicate: CardPredicate): Boolean {
        val typeLine = card.typeLine
        // CR 202.3e / the companion rulings: X in a card's mana cost counts as 0 off the stack.
        val manaValue = card.manaCost.cmc
        return when (predicate) {
            is CardPredicate.And -> predicate.predicates.all { matchesPrinted(card, it) }
            is CardPredicate.Or -> predicate.predicates.any { matchesPrinted(card, it) }
            is CardPredicate.Not -> !matchesPrinted(card, predicate.predicate)
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
            // A card in a deck is never a token.
            CardPredicate.IsToken -> false
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
            CardPredicate.HasXInManaCost -> card.manaCost.hasX
            // Zirda's "has an activated ability" in deck construction (ruling): any printed
            // activated ability in any zone — cycling included, since "some keyword abilities are
            // activated abilities" — and the intrinsic mana ability of a basic land type.
            CardPredicate.HasActivatedAbility ->
                card.script.activatedAbilities.isNotEmpty() || card.hasCycling ||
                    (typeLine.isLand && typeLine.subtypes.any { it.value in Subtype.ALL_BASIC_LAND_TYPES })
            else -> false
        }
    }

    private fun more(count: Int): String = if (count > MAX_NAMED) " and ${count - MAX_NAMED} more" else ""

    private const val MAX_NAMED = 5
}
