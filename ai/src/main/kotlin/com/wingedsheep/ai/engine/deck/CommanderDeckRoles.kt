package com.wingedsheep.ai.engine.deck

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.EffectWalker
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.RemovalBreadth
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.ai.engine.knowledge.SynergyProfile
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

    /**
     * Answers something the opponents have: removal, a sweeper, a counterspell, a fight — one that
     * can aim at most of the table, not a hoser ([RemovalBreadth.isNarrow]).
     */
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
            // A hoser — Execute's "white creature", Choking Sands' "non-Swamp land" — answers too
            // little of what an unknown opponent plays to fill an interaction slot.
            if (tags.any { it in INTERACTION } && !intent.targetsOnlyOurPermanents && !RemovalBreadth.isNarrow(card)) {
                add(DeckRole.REMOVAL)
            }
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
 * What a commander asks of the rest of its deck, and how well a candidate answers. Nothing in it is
 * specific to the command zone: [ConstructedDeckGenerator] reads its 60-card deck's build-around
 * card the same way.
 *
 * Three readings of the commander, from strongest to weakest:
 *  - **Tribes.** A creature type its rules text names ("other Elves you control") is the deck's
 *    theme; one it merely *is* is a lean.
 *  - **Keywords.** A keyword its text rewards ("creatures you control with flying") or it shares.
 *  - **Themes.** The resources it makes or cashes in — creatures dying, tokens, creatures entering,
 *    life gain, instants and sorceries, +1/+1 counters, the graveyard — read structurally by
 *    [SynergyAxes]: a candidate scores on every axis where one of the two feeds what the other
 *    rewards. Artifacts and enchantments, which are a card type rather than a resource, are still
 *    read off the commander's text.
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

    /**
     * Creature types the commander asks for: the ones its text names, singular or plural, and the
     * ones its abilities' filters name ([SynergyProfile.tribesRewarded]) — which also catches a card
     * whose printed text spells the type differently from the subtype.
     */
    val namedTribes: Set<Subtype> = Subtype.ALL_CREATURE_TYPES
        .filter { type -> mentions(type) || mentions(pluralNounPhrase(type)) }
        .map { Subtype(it) }
        .toSet() + SynergyAxes.of(commander).tribesRewarded - SynergyAxes.of(commander).punishes.keys

    /** Creature types the commander shrinks ("all Goblins get -1/-1"): their members score below 0. */
    private val punishedTribes: Set<Subtype> = SynergyAxes.of(commander).punishes.keys

    /**
     * How much the commander gives back per card that answers it ([SynergyProfile.strength]),
     * held to [MIN_STRENGTH]–[MAX_STRENGTH] here: a weak lord still names a tribe worth building,
     * and a strong one shouldn't drown out every off-theme bomb.
     */
    private val strength: Double = SynergyAxes.of(commander).strength.coerceIn(MIN_STRENGTH, MAX_STRENGTH)

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

    private val axes: SynergyProfile = SynergyAxes.of(commander).let {
        // Every creature commander "feeds" creatures-entering once, by being cast; that is not a theme.
        it.copy(feeds = it.feeds - SynergyAxis.CREATURES_ENTER)
    }

    private val rewardsArtifacts = Regex("""\bartifacts?\b""").containsMatchIn(text)
    private val rewardsEnchantments = Regex("""\benchantments?\b""").containsMatchIn(text)

    /**
     * True when the card's *text* asks the deck for something — a tribe it names, a keyword it
     * rewards, a theme it does or pays off — rather than only sharing a creature type or keyword.
     * The constructed generator reads a build-around through this class too, and only a card that
     * asks for something is a plan worth building towards.
     */
    val asksForSomething: Boolean = namedTribes.isNotEmpty() || rewardedKeywords.isNotEmpty() ||
        axes.rewards.isNotEmpty() || (axes.feeds intersect THEME_FEEDS).isNotEmpty() ||
        rewardsArtifacts || rewardsEnchantments

    /** True when the commander asks for anything at all; a vanilla legend scores every card 0. */
    val hasTheme: Boolean = namedTribes.isNotEmpty() || ownTribes.isNotEmpty() || rewardedKeywords.isNotEmpty() ||
        sharedKeywords.isNotEmpty() || !axes.isEmpty || rewardsArtifacts || rewardsEnchantments

    /**
     * How much [card] does what the commander wants — 0 for nothing, roughly 1 per independent
     * hit, with a creature of a named tribe counting five — scaled by the commander's [strength],
     * so the Goblins beside a +2/+2 lord pull harder than beside a +1/+1 one. [PUNISHED] for a
     * creature of a type the commander shrinks, which no hit makes up for. Capped by the caller.
     */
    fun score(card: CardDefinition): Double {
        val changeling = Keyword.CHANGELING in card.keywords
        val tribes = card.typeLine.subtypes
        if (punishedTribes.isNotEmpty() && card.typeLine.isCreature && (changeling || tribes.any { it in punishedTribes })) {
            return PUNISHED
        }
        return hits(card, changeling, tribes) * strength
    }

    private fun hits(card: CardDefinition, changeling: Boolean, tribes: Set<Subtype>): Double {
        var score = 0.0
        val cardText = card.oracleText.lowercase()
        if (namedTribes.isNotEmpty()) {
            when {
                changeling || tribes.any { it in namedTribes } -> score += 5.0
                namedTribePatterns.any { it.containsMatchIn(cardText) } -> score += 3.0
                SynergyAxes.of(card).let { read -> (read.tribes + read.tribesRewarded).any { it in namedTribes } } ->
                    score += 3.0
            }
        } else if (ownTribes.isNotEmpty() && (changeling || tribes.any { it in ownTribes })) {
            score += 1.0
        }
        if (card.keywords.any { it in rewardedKeywords }) score += 2.5
        else if (card.keywords.any { it in sharedKeywords }) score += 0.5
        if (!axes.isEmpty) score += axes.sharedWith(SynergyAxes.of(card)).size.coerceAtMost(MAX_THEME_AXES)
        if (rewardsArtifacts && card.typeLine.isArtifact) score += 1.0
        if (rewardsEnchantments && card.typeLine.isEnchantment) score += 1.0
        return score
    }

    private fun mentions(word: String): Boolean =
        Regex("""\b${Regex.escape(word.lowercase())}\b""").containsMatchIn(text)


    private companion object {
        const val MIN_STRENGTH = 0.5
        const val MAX_STRENGTH = 1.5

        /**
         * [score] of a creature the commander shrinks: enough to sink its sampling weight in both
         * generators' `× (1 + weight × score)` (Commander's weight is 2), never below zero.
         */
        const val PUNISHED = -0.4

        /** Theme axes one candidate can score on; past two it is already a theme card. */
        const val MAX_THEME_AXES = 2

        /**
         * What a card *does* that is a deck theme on its own — making tokens, gaining life,
         * sacrificing, filling the graveyard — as opposed to feeding an axis in passing (every
         * instant feeds the spells axis; that alone is not a plan to build towards).
         */
        val THEME_FEEDS = setOf(SynergyAxis.TOKENS, SynergyAxis.LIFEGAIN, SynergyAxis.DEATH, SynergyAxis.GRAVEYARD)

        /** Keywords too common to call a shared one synergy — every third creature has one. */
        fun Keyword.isEvergreenCombat(): Boolean = this in setOf(
            Keyword.FLYING, Keyword.TRAMPLE, Keyword.VIGILANCE, Keyword.HASTE, Keyword.REACH,
            Keyword.FIRST_STRIKE, Keyword.DEATHTOUCH, Keyword.LIFELINK, Keyword.MENACE,
        )
    }
}
