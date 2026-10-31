package com.wingedsheep.ai.draftsim

import com.wingedsheep.ai.engine.knowledge.DeckSynergy
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyProfile
import com.wingedsheep.sdk.model.CardDefinition

/**
 * These tables plus the structural synergy reading ([SynergyAxes]) of every nonland card in
 * [cards], so the Draftsim scorers can see enablers and payoffs on sets whose archetype tables don't
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
        val profile = SynergyAxes.of(card)
        if (!profile.isEmpty) read[key] = profile
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
        return if (tagged && read.rewards.isNotEmpty()) read.copy(rewards = emptySet()) else read
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
     *  - **as a feeder**, [FEED_WEIGHT] per payoff-share it fills ([DeckSynergy.feedGain]) — the
     *    second sacrifice outlet beside Blood Artist;
     *  - **as a payoff**, minus [UNDERFED_WEIGHT] per share it would go unfed, scaled by how far the
     *    draft is ([progress]): nothing in pack 1, when the fuel is still to come, all of it once the
     *    pool is complete. A payoff's rating assumes a deck that feeds it.
     *
     * Capped at [CAP] either way — below the ladder's one-rarity step (1.0), so a bomb still beats a
     * synergy common. Null when synergy is off or the card moves nothing.
     */
    fun pickAdjustment(card: ScorerCard, pool: List<ScorerCard>): Adjustment? {
        if (!enabled) return null
        val profile = profile(card)
        if (profile.isEmpty) return null
        val others = withoutOneCopy(pool, card).map(::profile)
        val gain = DeckSynergy.feedGain(profile, others)
        val unfed = if (profile.rewards.isEmpty()) 0.0
            else 1.0 - DeckSynergy.fedShare(profile, DeckSynergy.counts(others + profile))
        val points = (FEED_WEIGHT * gain - UNDERFED_WEIGHT * unfed * progress(others.size)).coerceIn(-CAP, CAP)
        if (points == 0.0) return null
        val axes = (if (points > 0) profile.feeds else profile.rewards).joinToString("/") { it.name.lowercase() }
        val reason = if (points > 0) "Feeds payoffs in pool ($axes)" else "Payoff short of fuel ($axes)"
        return Adjustment(round2(points), reason)
    }

    private fun withoutOneCopy(pool: List<ScorerCard>, card: ScorerCard): List<ScorerCard> {
        val index = pool.indexOfFirst { it.name == card.name }
        return if (index < 0) pool else pool.filterIndexed { i, _ -> i != index }
    }

    companion object {
        /** Pick value per payoff-share a feeder fills. */
        const val FEED_WEIGHT = 0.5

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
