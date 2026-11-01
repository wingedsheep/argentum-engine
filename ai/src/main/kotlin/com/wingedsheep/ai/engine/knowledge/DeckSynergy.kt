package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.GameObjectFilter
import java.util.EnumMap
import kotlin.math.min

/**
 * How well a **list of cards feeds its own payoffs**: the deck-building reading of [SynergyAxes],
 * shared by the draft pick scorer, the Draftsim deck scorer and the heuristic sealed builder.
 *
 * A payoff (a card that [SynergyProfile.rewards] an axis or asks for a creature type) is **fed** to
 * the extent the *other* cards feed one of its axes — [FEEDERS_WANTED] feeders make it fully fed,
 * fewer make it partly fed — or are its creature type: [TRIBE_MEMBERS_WANTED] Goblins (changelings
 * included) fully feed Goblin King — or are the kind of card it asks for ([CardAsks]): every
 * noncreature spell feeds Monastery Mentor, every artifact feeds Etherium Sculptor. A card
 * that rewards several axes counts its best-fed one — it only needs one to be live. A card never
 * feeds itself: Young Pyromancer's tokens don't count toward its own spells axis, and a second copy
 * of a payoff is a payoff, not fuel.
 *
 * Shares are counts; what a fed share is *worth* is the payoff's [SynergyProfile.strength] — a fed
 * +2/+2 lord is worth more than a fed +1/+0 one — and a card that shrinks a creature type
 * ([SynergyProfile.punishes]) loses for every member of it beside it ([punishment]).
 *
 * Every number here is a count of cards, never a game simulation. A limited deck's creatures already
 * feed "whenever another creature enters" whatever is drafted around them, so that axis is always
 * fed; the axes that separate decks are the ones with dedicated enablers — sacrifice outlets, token
 * makers, life gain, instants and sorceries, +1/+1 counters, self-mill.
 */
object DeckSynergy {

    /** Feeders a payoff needs among the rest of the deck to count as fully fed. */
    const val FEEDERS_WANTED = 3.0

    /**
     * Members a creature-type payoff needs to count as fully fed. More than an axis wants: a lord
     * pays per body it pumps, so three Goblins beside Goblin King is a start, not a tribe.
     */
    const val TRIBE_MEMBERS_WANTED = 5.0

    /**
     * Per-axis, per-creature-type and per-ask feeder counts over a list of cards, read by
     * [fedShare]. Asks are counted only for the filters the list's own payoffs ask with.
     */
    class Counts internal constructor(
        private val feeders: EnumMap<SynergyAxis, Int>,
        private val members: Map<Subtype, Int>,
        private val everyTribe: Int,
        private val answers: Map<GameObjectFilter, Int>,
    ) {
        fun feedersOf(axis: SynergyAxis): Int = feeders[axis] ?: 0
        fun membersOf(tribe: Subtype): Int = (members[tribe] ?: 0) + everyTribe
        fun answersTo(ask: CardAsk): Int = answers[ask.filter] ?: 0
    }

    fun counts(cards: List<SynergyProfile>): Counts {
        val feeders = EnumMap<SynergyAxis, Int>(SynergyAxis::class.java)
        val members = HashMap<Subtype, Int>()
        var everyTribe = 0
        for (card in cards) {
            for (axis in card.feeds) feeders.merge(axis, 1, Int::plus)
            if (card.everyTribe) everyTribe++ else for (tribe in card.tribes) members.merge(tribe, 1, Int::plus)
        }
        val answers = HashMap<GameObjectFilter, Int>()
        for (ask in cards.flatMap { it.asks }) {
            if (ask.filter !in answers) answers[ask.filter] = cards.count { it.answers(ask) }
        }
        return Counts(feeders, members, everyTribe, answers)
    }

    /**
     * How fed [payoff] is, 0–1, beside the cards [counts] was taken over: its best-fed axis,
     * creature type or ask. Those cards include [payoff] itself, so its own contribution is subtracted.
     * Zero for a card that is no payoff.
     */
    fun fedShare(payoff: SynergyProfile, counts: Counts): Double {
        if (!payoff.isPayoff) return 0.0
        val axes = payoff.rewards.maxOfOrNull { axis ->
            val own = if (axis in payoff.feeds) 1 else 0
            min(1.0, (counts.feedersOf(axis) - own).coerceAtLeast(0) / FEEDERS_WANTED)
        } ?: 0.0
        val tribes = payoff.tribesRewarded.maxOfOrNull { tribe ->
            val own = if (payoff.feedsTribe(tribe)) 1 else 0
            min(1.0, (counts.membersOf(tribe) - own).coerceAtLeast(0) / TRIBE_MEMBERS_WANTED)
        } ?: 0.0
        val asks = payoff.asks.maxOfOrNull { ask ->
            val own = if (payoff.answers(ask)) 1 else 0
            min(1.0, (counts.answersTo(ask) - own).coerceAtLeast(0) / ask.wanted)
        } ?: 0.0
        return maxOf(axes, tribes, asks)
    }

    /**
     * What [card]'s synergy is worth beside the cards [counts] was taken over, in payoff-equivalents:
     * its [fedShare] weighed by its [SynergyProfile.strength] — a fed +2/+2 lord counts for more than
     * a fed +1/+1 one — less its [punishment] of its own deck.
     */
    fun fedValue(card: SynergyProfile, counts: Counts): Double =
        (if (card.isPayoff) fedShare(card, counts) * card.strength else 0.0) - punishment(card, counts)

    /**
     * What a card that shrinks a creature type ([SynergyProfile.punishes]) costs beside the cards
     * [counts] was taken over: per type, the share of a tribe ([TRIBE_MEMBERS_WANTED]) it shrinks,
     * weighed by how hard. Zero for a card that shrinks nothing, or nothing in this deck.
     */
    fun punishment(card: SynergyProfile, counts: Counts): Double =
        card.punishes.entries.sumOf { (tribe, weight) ->
            val own = if (card.feedsTribe(tribe)) 1 else 0
            min(1.0, (counts.membersOf(tribe) - own).coerceAtLeast(0) / TRIBE_MEMBERS_WANTED) * weight
        }

    /** How many payoff-equivalents [cards] keep fed: the sum of every card's [fedValue]. */
    fun fedPayoffs(cards: List<SynergyProfile>): Double {
        val counts = counts(cards)
        return cards.sumOf { fedValue(it, counts) }
    }

    /**
     * How short [cards] leave their payoffs: per payoff, the share it is *not* fed weighed by its
     * [SynergyProfile.strength], summed, plus every [punishment]. A payoff's rating assumes a deck
     * that feeds it, so this is what the deck loses against those ratings — the more a payoff gives
     * per feeder, the more it loses unfed — except a payoff that [SynergyProfile.standsAlone], whose
     * rating is earned as an answer.
     */
    fun shortfall(cards: List<SynergyProfile>): Double {
        val counts = counts(cards)
        return cards.sumOf { unfed(it, counts) * it.strength + punishment(it, counts) }
    }

    /** The share of [payoff] left unfed beside [counts], or zero for a card that needs no fuel. */
    fun unfed(payoff: SynergyProfile, counts: Counts): Double =
        if (!payoff.isPayoff || payoff.standsAlone) 0.0 else 1.0 - fedShare(payoff, counts)

    /**
     * How much adding [card] to [others] raises their synergy: the [fedValue] it adds to the other
     * cards, as a feeder — the reason to take the second sacrifice outlet, and more of a reason
     * beside a strong payoff than a weak one. Negative for a creature whose type another card
     * shrinks: a Goblin beside "all Goblins get -1/-1". A payoff's own value is not counted:
     * adding a payoff opens a shortfall of its own, which is [shortfall]'s business.
     */
    fun feedGain(card: SynergyProfile, others: List<SynergyProfile>): Double {
        if (!card.isFeeder) return 0.0
        val before = counts(others)
        val after = counts(others + card)
        return others.sumOf { other ->
            if (!other.isPayoff && !other.isPunisher) 0.0
            else fedValue(other, after) - fedValue(other, before)
        }
    }
}
