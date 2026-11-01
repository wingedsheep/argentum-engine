package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import java.util.concurrent.ConcurrentHashMap

/**
 * How much of what an opponent might have a card's answers can actually aim at: Murder hits every
 * creature, Doom Blade all but the black ones, Execute only the white ones, Perish only the green
 * ones, Choking Sands no creature at all (it destroys a land), and Coffin Purge nothing on the
 * table (it exiles a card from a graveyard).
 *
 * Read off each removal or counterspell slot: the target filters beside it and the group a sweeper
 * sweeps, matched ([PrintedFilter]) against a fixed set of probes — a vanilla creature of each
 * colour and colourless at three sizes, plus an artifact, an enchantment and a planeswalker (for a
 * counterspell's spell target, an instant and a sorcery instead of the planeswalker). A target in a
 * graveyard, hand or library is no answer at all ([answersOnTable]). [of] is the larger of the creature share and the
 * noncreature share for the widest slot, so Naturalize, which answers two of the three
 * noncreature threats, is wide, and a hoser that names one colour is narrow.
 *
 * It fails open: a slot with no target or swept group (an edict, "any target"), a filter the probes
 * can't read, and a predicate only a game can answer (tapped, attacking, "a spell that targets…")
 * all count as reaching everything. Being wrong in that direction leaves a card where it was; the
 * reading only ever discounts a card whose filter plainly excludes most of the table.
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
        removalSlots(card).mapNotNull(::ofSlot).maxOrNull() ?: 1.0
    }

    /** Whether [card]'s removal is too narrow to count as an answer ([NARROW_BELOW]). */
    fun isNarrow(card: CardDefinition): Boolean = of(card) < NARROW_BELOW

    /**
     * False when every removal slot of [card] aims only at cards off the table — Coffin Purge's
     * "card in a graveyard", Treasured Find's own card — so whatever [IntentTag.REMOVAL] the
     * analyzer reads off it answers nothing an opponent has in play. True when any slot aims at the
     * table, and for a card whose removal sits outside the slots this reads.
     */
    fun answersOnTable(card: CardDefinition): Boolean = onTable.getOrPut(card.name) {
        val slots = removalSlots(card)
        slots.isEmpty() || slots.any { ofSlot(it) != null }
    }

    private val onTable = ConcurrentHashMap<String, Boolean>()

    private fun removalSlots(card: CardDefinition): List<EffectWalker.Slot> =
        (listOf(card.script) + card.cardFaces.map { it.script }).flatMap(EffectWalker::slots).filter { isRemoval(it.effect) }

    /**
     * The breadth of one slot: the widest thing it aims across the table — a permanent target, a
     * spell target, or the group a sweeper sweeps ("destroy all white creatures") — or 1.0 when it
     * names none (a bound or untargeted effect, which this can't read). Null when it aims only at
     * cards off the table (a graveyard, hand or library): there is no breadth to speak of, and the
     * removal reading itself is suspect.
     */
    fun ofSlot(slot: EffectWalker.Slot): Double? {
        val aimed = slot.targets.filterIsInstance<TargetObject>().filterNot { ours(it.filter) }
        val swept = EffectWalker.leaves(slot.effect).mapNotNull(::sweptGroup).filterNot(::ours)
        if (aimed.isEmpty() && swept.isEmpty()) return 1.0
        val breadths = aimed.mapNotNull { target ->
            val filters = listOf(target.filter.baseFilter) + target.filter.alternatives.map { it.baseFilter }
            when (target.filter.zone) {
                Zone.BATTLEFIELD -> breadth(filters, PERMANENT_PROBES)
                Zone.STACK -> breadth(filters, SPELL_PROBES)
                else -> null
            }
        } + swept.map { breadth(listOf(it), PERMANENT_PROBES) }
        return breadths.maxOrNull()
    }

    /**
     * Whether [effect] answers something — takes a permanent away ([IntentTag.REMOVAL]) or counters
     * a spell ([IntentTag.COUNTERSPELL]).
     */
    fun isRemoval(effect: Effect): Boolean =
        CardIntentAnalyzer.analyzeEffect(effect).tags.let { IntentTag.REMOVAL in it || IntentTag.COUNTERSPELL in it }

    /**
     * The battlefield group a sweeper iterates or gathers: Wrath of God's "for each creature",
     * Virtue's Ruin's "destroy all white creatures" pipeline.
     */
    private fun sweptGroup(leaf: Effect): GameObjectFilter? = when (leaf) {
        is ForEachEffect -> (leaf.space as? IterationSpace.Group)?.filter
            ?.takeIf { it.scope is Scope.Battlefield }?.baseFilter
        is GatherCardsEffect -> (leaf.source as? CardSource.BattlefieldMatching)
            ?.takeIf { it.player != Player.You }?.filter
        else -> null
    }

    /**
     * The share of [probes] any of [filters] can hit (a union: "creature or planeswalker"): the
     * larger of the creature share and the noncreature share.
     */
    private fun breadth(filters: List<GameObjectFilter>, probes: Probes): Double {
        fun hits(probe: CardDefinition): Boolean = filters.any { PrintedFilter.matches(probe, it) != false }
        val creatures = probes.creatures.count(::hits).toDouble() / probes.creatures.size
        val others = probes.others.count(::hits).toDouble() / probes.others.size
        return maxOf(creatures, others)
    }

    private class Probes(val creatures: List<CardDefinition>, val others: List<CardDefinition>)

    private fun ours(filter: TargetFilter): Boolean = filter.alternatives.isEmpty() && ours(filter.baseFilter)

    private fun ours(filter: GameObjectFilter): Boolean = filter.controllerPredicate == ControllerPredicate.ControlledByYou

    /** A vanilla creature of each colour and colourless, at three sizes. */
    private val CREATURE_PROBES: List<CardDefinition> = buildList {
        for (pip in listOf("{W}", "{U}", "{B}", "{R}", "{G}", "")) for (size in listOf(1, 3, 6)) {
            val generic = if (pip.isEmpty()) size else size - 1
            val cost = (if (generic > 0) "{$generic}" else "") + pip
            add(CardDefinition.creature("Breadth probe $pip$size", ManaCost.parse(cost), emptySet(), size, size))
        }
    }

    private val ARTIFACT = CardDefinition.artifact("Breadth probe artifact", ManaCost.parse("{3}"))
    private val ENCHANTMENT = CardDefinition.enchantment("Breadth probe enchantment", ManaCost.parse("{2}{G}"))

    /** The permanents removal is played to answer: creatures, plus the noncreature threats. */
    private val PERMANENT_PROBES = Probes(
        CREATURE_PROBES,
        listOf(
            ARTIFACT,
            ENCHANTMENT,
            CardDefinition.planeswalker("Breadth probe planeswalker", ManaCost.parse("{3}{U}"), setOf(Subtype("Jace")), 4),
        ),
    )

    /** The spells a counterspell is played to answer: creature spells, plus the rest. */
    private val SPELL_PROBES = Probes(
        CREATURE_PROBES,
        listOf(
            ARTIFACT,
            ENCHANTMENT,
            CardDefinition.instant("Breadth probe instant", ManaCost.parse("{1}{U}"), ""),
            CardDefinition.sorcery("Breadth probe sorcery", ManaCost.parse("{2}{B}"), ""),
        ),
    )
}
