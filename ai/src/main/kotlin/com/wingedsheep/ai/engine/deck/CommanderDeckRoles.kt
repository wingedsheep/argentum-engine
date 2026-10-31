package com.wingedsheep.ai.engine.deck

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.EffectWalker
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.producedColors
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.util.pluralNounPhrase

/**
 * The jobs a nonland card does in a hundred-card singleton deck — the slots a Commander deck counts
 * before it counts anything else.
 *
 * A reading over [CardIntentAnalyzer], not a second classifier: the analyzer already knows a card
 * destroys, counters, draws or makes mana. This only decides which of those readings fill a
 * *deck-construction* quota, which is a narrower question — a ritual makes mana but is not ramp, a
 * cantrip draws but is not card advantage, and a "search for a basic land" spell the analyzer calls
 * a tutor is the most common ramp there is.
 */
internal enum class DeckRole {
    /** Leaves extra mana behind: a mana rock or dork, or a spell that puts a land into play. */
    RAMP,

    /** Answers something the opponents have: removal, a sweeper, a counterspell, a fight. */
    REMOVAL,

    /** Card advantage: draws two or more at once, or keeps drawing. */
    DRAW,
}

internal object CommanderDeckRoles {

    private val INTERACTION = setOf(
        IntentTag.REMOVAL, IntentTag.EXILE_REMOVAL, IntentTag.SWEEPER,
        IntentTag.NEUTRALIZE, IntentTag.FIGHT, IntentTag.COUNTERSPELL,
    )

    private val BASIC_LAND_TYPES = setOf(
        Subtype.PLAINS, Subtype.ISLAND, Subtype.SWAMP, Subtype.MOUNTAIN, Subtype.FOREST,
    )

    fun rolesOf(card: CardDefinition): Set<DeckRole> {
        if (card.isLand) return emptySet()
        val intent = CardIntentAnalyzer.analyze(card)
        val tags = intent.tags
        val permanent = !card.typeLine.isInstant && !card.typeLine.isSorcery
        return buildSet {
            // A ritual's mana is gone by end of turn; only a permanent's mana, or a land, ramps.
            if ((IntentTag.RAMP in tags && permanent) || landSearch(card) != null) add(DeckRole.RAMP)
            if (tags.any { it in INTERACTION } && !intent.targetsOnlyOurPermanents) add(DeckRole.REMOVAL)
            if (IntentTag.DRAW in tags && ((intent.cardsDrawn ?: 0) >= 2 || intent.repeatable)) add(DeckRole.DRAW)
        }
    }

    /**
     * Whether a ramp card also *fixes* a three-plus-colour deck: it makes at least two of the
     * deck's colours, or fetches a basic of whichever type the deck is short of.
     */
    fun fixes(card: CardDefinition, identity: Set<Color>): Boolean {
        landSearch(card)?.let { predicates ->
            return predicates.none { it is CardPredicate.HasSubtype || it is CardPredicate.HasAnyOfSubtypes }
        }
        return producedColors(card).count { it in identity } >= 2
    }

    /**
     * The predicates of a "search your library for a land" this card performs, or null — the
     * Rampant Growth / Cultivate / Wood Elves shape the analyzer files under [IntentTag.TUTOR].
     */
    private fun landSearch(card: CardDefinition): List<CardPredicate>? =
        (listOf(card.script) + card.cardFaces.map { it.script })
            .flatMap { EffectWalker.slots(it) }
            .flatMap { EffectWalker.leaves(it.effect) }
            .firstNotNullOfOrNull { effect ->
                val source = (effect as? GatherCardsEffect)?.source as? CardSource.FromZone
                val predicates = source?.takeIf { it.zone == Zone.LIBRARY }?.filter?.cardPredicates.orEmpty()
                predicates.takeIf { it.isNotEmpty() && it.all(::isLandPredicate) }
            }

    private fun isLandPredicate(predicate: CardPredicate): Boolean = when (predicate) {
        CardPredicate.IsLand, CardPredicate.IsBasicLand -> true
        is CardPredicate.HasSubtype -> predicate.subtype in BASIC_LAND_TYPES
        is CardPredicate.HasAnyOfSubtypes -> predicate.subtypes.all { it in BASIC_LAND_TYPES }
        else -> false
    }
}

/**
 * What a commander asks of the rest of its deck, and how well a candidate answers.
 *
 * Three readings of the commander, from strongest to weakest:
 *  - **Tribes.** A creature type its rules text names ("other Elves you control") is the deck's
 *    theme; one it merely *is* is a lean.
 *  - **Keywords.** A keyword its text rewards ("creatures you control with flying") or it shares.
 *  - **Themes.** What its abilities do or pay off — tokens, lifegain, sacrifice, recursion,
 *    +1/+1 counters, artifacts, enchantments, instants and sorceries. Read from its
 *    [CardIntentAnalyzer] tags where the analyzer has them and from the payoff words of its text
 *    ("whenever you gain life", "whenever a creature you control dies") where it doesn't, because a
 *    payoff is a trigger condition the analyzer doesn't tag.
 *
 * [score] is a small count of how many of those a candidate hits; the generator turns it into a
 * sampling weight, so a strong card off-theme still makes the deck and an on-theme card beats an
 * equally rated generic one.
 */
internal class CommanderSynergy(commander: CardDefinition) {

    private val text: String = commander.oracleText
        .replace(commander.name, " ")
        .replace(commander.name.substringBefore(','), " ")
        .lowercase()

    private val ownTribes: Set<Subtype> =
        commander.typeLine.subtypes.filter { it.value in Subtype.ALL_CREATURE_TYPES }.toSet()

    /** Creature types the commander's text names, singular or plural. */
    val namedTribes: Set<Subtype> = Subtype.ALL_CREATURE_TYPES
        .filter { type -> mentions(type) || mentions(pluralNounPhrase(type)) }
        .map { Subtype(it) }
        .toSet()

    /** "Elf"/"Elves" as word patterns, compiled once — [score] runs them over the whole pool. */
    private val namedTribePatterns: List<Regex> = namedTribes.flatMap { listOf(it.value, pluralNounPhrase(it.value)) }
        .distinct()
        .map { Regex("""\b${Regex.escape(it.lowercase())}\b""") }

    private val rewardedKeywords: Set<Keyword> = Keyword.entries.filter { kw ->
        val name = Regex.escape(kw.displayName.lowercase())
        Regex("""\b(with|have|has|gain|gains|without)\s+$name\b""").containsMatchIn(text)
    }.toSet()

    private val sharedKeywords: Set<Keyword> = commander.keywords - Keyword.entries
        .filter { it.isEvergreenCombat() }
        .toSet()

    private val themes: Set<IntentTag> = buildSet {
        addAll(CardIntentAnalyzer.analyze(commander).tags.filter { it in THEME_TAGS })
        if (IntentTag.ANTHEM in CardIntentAnalyzer.analyze(commander).tags) add(IntentTag.TOKEN_MAKER)
        if (Regex("""\bgains? life\b|\byou gain\b""").containsMatchIn(text)) add(IntentTag.LIFEGAIN)
        if ("token" in text) add(IntentTag.TOKEN_MAKER)
        if (Regex("""\bdies\b|\bsacrifice""").containsMatchIn(text)) {
            add(IntentTag.SACRIFICE_OUTLET)
            add(IntentTag.TOKEN_MAKER)
        }
        if ("graveyard" in text) add(IntentTag.RECURSION)
    }

    private val rewardsCounters = "+1/+1 counter" in text
    private val rewardsArtifacts = Regex("""\bartifacts?\b""").containsMatchIn(text)
    private val rewardsEnchantments = Regex("""\benchantments?\b""").containsMatchIn(text)
    private val rewardsSpells = Regex("""instant (or|and) sorcery|noncreature spell""").containsMatchIn(text)

    /** True when the commander asks for anything at all; a vanilla legend scores every card 0. */
    val hasTheme: Boolean = namedTribes.isNotEmpty() || ownTribes.isNotEmpty() || rewardedKeywords.isNotEmpty() ||
        sharedKeywords.isNotEmpty() || themes.isNotEmpty() || rewardsCounters || rewardsArtifacts ||
        rewardsEnchantments || rewardsSpells

    /**
     * How much [card] does what the commander wants — 0 for nothing, roughly 1 per independent
     * hit, with a creature of a named tribe counting five. Capped by the caller.
     */
    fun score(card: CardDefinition): Double {
        var score = 0.0
        val changeling = Keyword.CHANGELING in card.keywords
        val tribes = card.typeLine.subtypes
        val cardText = card.oracleText.lowercase()
        if (namedTribes.isNotEmpty()) {
            when {
                changeling || tribes.any { it in namedTribes } -> score += 5.0
                namedTribePatterns.any { it.containsMatchIn(cardText) } -> score += 3.0
            }
        } else if (ownTribes.isNotEmpty() && (changeling || tribes.any { it in ownTribes })) {
            score += 1.0
        }
        if (card.keywords.any { it in rewardedKeywords }) score += 2.5
        else if (card.keywords.any { it in sharedKeywords }) score += 0.5
        if (themes.isNotEmpty()) {
            score += CardIntentAnalyzer.analyze(card).tags.count { it in themes }.coerceAtMost(2)
        }
        if (rewardsCounters && "+1/+1 counter" in cardText) score += 1.0
        if (rewardsArtifacts && card.typeLine.isArtifact) score += 1.0
        if (rewardsEnchantments && card.typeLine.isEnchantment) score += 1.0
        if (rewardsSpells && (card.typeLine.isInstant || card.typeLine.isSorcery)) score += 1.0
        return score
    }

    private fun mentions(word: String): Boolean =
        Regex("""\b${Regex.escape(word.lowercase())}\b""").containsMatchIn(text)


    private companion object {
        /** Intent tags that are a deck *theme* when the commander has them — not just a job. */
        val THEME_TAGS = setOf(
            IntentTag.TOKEN_MAKER, IntentTag.LIFEGAIN, IntentTag.SACRIFICE_OUTLET, IntentTag.RECURSION,
        )

        /** Keywords too common to call a shared one synergy — every third creature has one. */
        fun Keyword.isEvergreenCombat(): Boolean = this in setOf(
            Keyword.FLYING, Keyword.TRAMPLE, Keyword.VIGILANCE, Keyword.HASTE, Keyword.REACH,
            Keyword.FIRST_STRIKE, Keyword.DEATHTOUCH, Keyword.LIFELINK, Keyword.MENACE,
        )
    }
}
