package com.wingedsheep.ai.engine.knowledge

import java.util.EnumMap
import kotlin.math.min

/**
 * How well a **list of cards feeds its own payoffs**: the deck-building reading of [SynergyAxes],
 * shared by the draft pick scorer, the Draftsim deck scorer and the heuristic sealed builder.
 *
 * A payoff (a card that [SynergyProfile.rewards] an axis) is **fed** to the extent the *other* cards
 * feed one of its axes: [FEEDERS_WANTED] feeders make it fully fed, fewer make it partly fed. A card
 * that rewards several axes counts its best-fed one — it only needs one to be live. A card never
 * feeds itself: Young Pyromancer's tokens don't count toward its own spells axis, and a second copy
 * of a payoff is a payoff, not fuel.
 *
 * Every number here is a count of cards, never a game simulation. A limited deck's creatures already
 * feed "whenever another creature enters" whatever is drafted around them, so that axis is always
 * fed; the axes that separate decks are the ones with dedicated enablers — sacrifice outlets, token
 * makers, life gain, instants and sorceries, +1/+1 counters, self-mill.
 */
object DeckSynergy {

    /** Feeders a payoff needs among the rest of the deck to count as fully fed. */
    const val FEEDERS_WANTED = 3.0

    /** Per-axis feeder counts over a list of cards, read by [fedShare]. */
    class Counts internal constructor(private val feeders: EnumMap<SynergyAxis, Int>) {
        fun feedersOf(axis: SynergyAxis): Int = feeders[axis] ?: 0
    }

    fun counts(cards: List<SynergyProfile>): Counts {
        val feeders = EnumMap<SynergyAxis, Int>(SynergyAxis::class.java)
        for (card in cards) for (axis in card.feeds) feeders.merge(axis, 1, Int::plus)
        return Counts(feeders)
    }

    /**
     * How fed [payoff] is, 0–1, beside the cards [counts] was taken over. Those cards include
     * [payoff] itself, so its own feeds are subtracted. Zero for a card that rewards nothing.
     */
    fun fedShare(payoff: SynergyProfile, counts: Counts): Double {
        if (payoff.rewards.isEmpty()) return 0.0
        return payoff.rewards.maxOf { axis ->
            val own = if (axis in payoff.feeds) 1 else 0
            min(1.0, (counts.feedersOf(axis) - own).coerceAtLeast(0) / FEEDERS_WANTED)
        }
    }

    /** How many payoff-equivalents [cards] keep fed: the sum of every payoff's [fedShare]. */
    fun fedPayoffs(cards: List<SynergyProfile>): Double {
        val counts = counts(cards)
        return cards.sumOf { fedShare(it, counts) }
    }

    /**
     * How short [cards] leave their payoffs: per payoff, the share it is *not* fed, summed. A payoff's
     * rating assumes a deck that feeds it, so this is what the deck loses against those ratings.
     */
    fun shortfall(cards: List<SynergyProfile>): Double {
        val counts = counts(cards)
        return cards.sumOf { if (it.rewards.isEmpty()) 0.0 else 1.0 - fedShare(it, counts) }
    }

    /**
     * How much adding [card] to [others] closes the deck's [shortfall]: the share it fills in other
     * payoffs, as a feeder — the reason to take the second sacrifice outlet. A payoff's own fed share
     * is not counted: adding a payoff opens a shortfall of its own, which is [shortfall]'s business.
     */
    fun feedGain(card: SynergyProfile, others: List<SynergyProfile>): Double {
        if (card.feeds.isEmpty()) return 0.0
        val before = counts(others)
        val after = counts(others + card)
        return others.sumOf { payoff ->
            if (payoff.rewards.isEmpty()) 0.0
            else fedShare(payoff, after) - fedShare(payoff, before)
        }
    }
}
