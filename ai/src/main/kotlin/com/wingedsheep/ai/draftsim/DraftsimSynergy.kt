package com.wingedsheep.ai.draftsim

import com.wingedsheep.ai.engine.knowledge.DeckSynergy
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyProfile
import com.wingedsheep.sdk.model.CardDefinition

/**
 * These tables plus the structural synergy reading ([SynergyAxes]) of every nonland card in
 * [cards] — every one, since any card may be what some payoff asks for — so the Draftsim scorers can see enablers and payoffs on sets whose archetype tables don't
 * list them — which is most sets, and every card in a set we ship no archetype file for.
 *
 * The tables' [DraftsimSetTables.axes] stay empty without this call, and every score then matches
 * the ported bundle exactly; that is the synergy-off build the measurement compares against.
 */
fun DraftsimSetTables.withSynergyAxes(cards: Collection<CardDefinition>): DraftsimSetTables {
    val read = LinkedHashMap<String, SynergyProfile>()
    for (card in cards) {
        if (card.typeLine.isLand) continue
        val key = DraftsimData.nameKey(card.name)
        if (key in axes || key in read) continue
        read[key] = SynergyAxes.of(card)
    }
    if (read.isEmpty()) return this
    return copy(axes = axes + read)
}

/**
 * The Draftsim scorers' reading of [DeckSynergy] over [ScorerCard]s: the deck's fed payoffs for the
 * synergy sub-score, and the pick-time adjustment for one card against a pool.
 *
 * A card the set's archetype table tags is already counted by the bundle's own enabler/payoff
 * arithmetic, so here it is fuel only — its rewards are dropped — and a payoff is never counted twice.
 */
internal class DraftsimSynergy(private val ops: DraftsimCardOps) {

    val enabled: Boolean get() = ops.tables.axes.isNotEmpty()

    fun profile(card: ScorerCard): SynergyProfile {
        val read = ops.tables.axes[DraftsimData.nameKey(card.name)] ?: return SynergyProfile.NONE
        val tagged = ops.archRecord(card.name)?.archetypes?.isNotEmpty() == true
        return if (tagged && read.isPayoff) read.copy(rewards = emptySet(), tribesRewarded = emptySet(), asks = emptyList()) else read
    }

    /** Payoff-equivalents [nonland] keeps fed ([DeckSynergy.fedPayoffs]); zero when synergy is off. */
    fun fedPayoffs(nonland: List<ScorerCard>): Double =
        if (!enabled) 0.0 else DeckSynergy.fedPayoffs(nonland.map(::profile))

    /** A pick-time adjustment and the reason line the scorer shows for it. */
    data class Adjustment(val points: Double, val reason: String)

    /**
     * What [card] is worth on top of its rating beside [pool] (the picks so far in a draft, the whole
     * pool in a deck build; a copy of [card] in it is set aside):
     *
     *  - **as a feeder**, [FEED_WEIGHT] per payoff-equivalent it adds ([DeckSynergy.feedGain]) — the
     *    second sacrifice outlet beside Blood Artist, the Goblin beside Goblin King, more beside a
     *    strong payoff than a weak one, and *less* than nothing beside a card that shrinks its type;
     *  - **as a payoff**, weighed by its [SynergyProfile.strength]: plus [FED_WEIGHT] per share the
     *    pool already feeds it — the engine is better for good fuel — and minus [UNDERFED_WEIGHT] per
     *    share it would go unfed, scaled by how far the draft is ([progress]): nothing in pack 1, when
     *    the fuel is still to come, all of it once the pool is complete;
     *  - **as a punisher**, minus [FEED_WEIGHT] per tribe-share of the pool it shrinks.
     *
     * Capped at [CAP] either way — below the ladder's one-rarity step (1.0), so a bomb still beats a
     * synergy common. Null when synergy is off or the card moves nothing.
     */
    fun pickAdjustment(card: ScorerCard, pool: List<ScorerCard>): Adjustment? {
        if (!enabled) return null
        val profile = profile(card)
        if (!profile.isFeeder && !profile.isPayoff && !profile.isPunisher) return null
        val others = withoutOneCopy(pool, card).map(::profile)
        val counts = DeckSynergy.counts(others + profile)
        val feeding = FEED_WEIGHT * DeckSynergy.feedGain(profile, others)
        val fed = if (profile.isPayoff) FED_WEIGHT * DeckSynergy.fedShare(profile, counts) * profile.strength else 0.0
        val unfed = UNDERFED_WEIGHT * DeckSynergy.unfed(profile, counts) * profile.strength * progress(others.size)
        val punishing = FEED_WEIGHT * DeckSynergy.punishment(profile, counts)
        val points = (feeding + fed - unfed - punishing).coerceIn(-CAP, CAP)
        if (points == 0.0) return null
        val wants = profile.rewards.map { it.name.lowercase() } + profile.tribesRewarded.map { it.value } +
            profile.asks.map { it.filter.description }
        val reason = when {
            feeding < 0 -> "Shrunk by a card in pool (${profile.tribes.joinToString("/") { it.value }})"
            punishing > 0 -> "Shrinks creatures in pool (${profile.punishes.keys.joinToString("/") { it.value }})"
            points > 0 && feeding >= fed -> {
                val asked = others.flatMap { it.asks }.filter(profile::answers).map { it.filter.description }
                "Feeds payoffs in pool (${(profile.feeds.map { it.name.lowercase() } + profile.tribes.map { it.value } + asked).distinct().joinToString("/")})"
            }
            points > 0 -> "Payoff fed by pool (${wants.joinToString("/")})"
            else -> "Payoff short of fuel (${wants.joinToString("/")})"
        }
        return Adjustment(round2(points), reason)
    }

    private fun withoutOneCopy(pool: List<ScorerCard>, card: ScorerCard): List<ScorerCard> {
        val index = pool.indexOfFirst { it.name == card.name }
        return if (index < 0) pool else pool.filterIndexed { i, _ -> i != index }
    }

    companion object {
        /** Pick value per payoff-equivalent a feeder adds, or a punisher takes away. */
        const val FEED_WEIGHT = 0.5

        /**
         * Pick value a payoff gains per share the pool feeds, at strength 1. Smaller than
         * [FEED_WEIGHT]: a payoff's rating already assumes some fuel.
         */
        const val FED_WEIGHT = 0.25

        /** Pick value a payoff loses per share it goes unfed, at full [progress]. */
        const val UNDERFED_WEIGHT = 0.75

        /** Largest adjustment either way: under one rarity step on the 0–5 rating scale. */
        const val CAP = 0.75

        /** Nonland picks by which a payoff's missing fuel starts to count: the end of pack 1. */
        const val UNDERFED_FROM = 14

        /** Nonland picks over which it ramps to full weight: the rest of a three-pack draft. */
        const val UNDERFED_RAMP = 28

        /** 0 through pack 1, 1 once the pool is a full draft's worth. */
        fun progress(nonlandPicks: Int): Double =
            ((nonlandPicks - UNDERFED_FROM).toDouble() / UNDERFED_RAMP).coerceIn(0.0, 1.0)
    }
}
