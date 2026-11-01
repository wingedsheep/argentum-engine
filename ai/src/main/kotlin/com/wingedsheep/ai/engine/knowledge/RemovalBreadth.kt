package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import java.util.concurrent.ConcurrentHashMap

/**
 * How much of what an opponent might have on the battlefield a card's removal can actually aim at:
 * Murder hits every creature, Doom Blade all but the black ones, Execute only the white ones, and
 * Choking Sands no creature at all — it destroys a land.
 *
 * Read off the target filters beside each removal effect, matched against a fixed set of probe
 * permanents ([PrintedFilter]): a vanilla creature of each colour and colourless at three sizes,
 * plus an artifact, an enchantment and a planeswalker. [of] is the larger of the share of creature
 * probes and the share of noncreature probes the best removal slot can target — so a Naturalize,
 * which answers two of the three noncreature threats, is wide, and a hoser that names one colour
 * is narrow.
 *
 * It fails open: a slot with no permanent target (a sweeper, an edict, "any target"), a filter the
 * probes can't read, and a predicate only a game can answer (tapped, attacking) all count as
 * reaching everything. Being wrong in that direction leaves a card where it was; the reading only
 * ever discounts a card whose filter plainly excludes most of the table.
 *
 * Not [CardIntent.removalReach], which is how *big* a creature a damage spell kills; this is how
 * many *kinds* of permanent the removal may aim at.
 *
 * Memoized per card name.
 */
object RemovalBreadth {

    /** Below this [of], removal is a hoser: it fills no interaction slot in a deck. */
    const val NARROW_BELOW = 0.3

    private val cache = ConcurrentHashMap<String, Double>()

    /** 0–1, the breadth of [card]'s widest removal slot; 1.0 for a card with no removal. */
    fun of(card: CardDefinition): Double = cache.getOrPut(card.name) {
        val scripts = listOf(card.script) + card.cardFaces.map { it.script }
        scripts.flatMap(EffectWalker::slots)
            .filter { slot -> EffectWalker.leaves(slot.effect).any(::isRemoval) }
            .maxOfOrNull { ofTargets(it.targets) }
            ?: 1.0
    }

    /** Whether [card]'s removal is too narrow to count as an answer ([NARROW_BELOW]). */
    fun isNarrow(card: CardDefinition): Boolean = of(card) < NARROW_BELOW

    /**
     * The breadth of one slot's [targets]: the widest permanent target it aims across the table, or
     * 1.0 when it aims at none (a bound or untargeted effect, which this can't read).
     */
    fun ofTargets(targets: List<TargetRequirement>): Double {
        val aimed = targets.filterIsInstance<TargetObject>().filterNot { ours(it.filter) }
        if (aimed.isEmpty()) return 1.0
        return aimed.maxOf { breadth(it.filter) }
    }

    /** Whether [leaf] takes a permanent away — the leaves [IntentTag.REMOVAL] is read from. */
    fun isRemoval(leaf: Effect): Boolean = IntentTag.REMOVAL in CardIntentAnalyzer.analyzeEffect(leaf).tags

    private fun breadth(filter: TargetFilter): Double {
        fun hits(probe: CardDefinition): Boolean =
            (listOf(filter) + filter.alternatives).any { PrintedFilter.matches(probe, it.baseFilter) != false }
        val creatures = CREATURE_PROBES.count(::hits).toDouble() / CREATURE_PROBES.size
        val others = NONCREATURE_PROBES.count(::hits).toDouble() / NONCREATURE_PROBES.size
        return maxOf(creatures, others)
    }

    private fun ours(filter: TargetFilter): Boolean =
        filter.alternatives.isEmpty() && filter.baseFilter.controllerPredicate == ControllerPredicate.ControlledByYou

    /** A vanilla creature of each colour and colourless, at three sizes. */
    private val CREATURE_PROBES: List<CardDefinition> = buildList {
        for (pip in listOf("{W}", "{U}", "{B}", "{R}", "{G}", "")) for (size in listOf(1, 3, 6)) {
            val generic = if (pip.isEmpty()) size else size - 1
            val cost = (if (generic > 0) "{$generic}" else "") + pip
            add(CardDefinition.creature("Breadth probe $pip$size", ManaCost.parse(cost), emptySet(), size, size))
        }
    }

    /** The noncreature permanents removal is played to answer. */
    private val NONCREATURE_PROBES: List<CardDefinition> = listOf(
        CardDefinition.artifact("Breadth probe artifact", ManaCost.parse("{3}")),
        CardDefinition.enchantment("Breadth probe enchantment", ManaCost.parse("{2}{G}")),
        CardDefinition.planeswalker("Breadth probe planeswalker", ManaCost.parse("{3}{U}"), setOf(Subtype("Jace")), 4),
    )
}
