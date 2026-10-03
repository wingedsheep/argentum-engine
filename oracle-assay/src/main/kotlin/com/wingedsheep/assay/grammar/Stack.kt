package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.constant
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.scripting.effects.CounterCondition
import com.wingedsheep.sdk.scripting.effects.CounterEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Clauses about the stack — countering a spell.
 *
 * The one clause family whose target is a *spell* rather than a permanent, a player or a card, which
 * is why it is its own file rather than a row in [Steps]: `TargetSpell` is a `TargetObject` scoped to
 * `Zone.STACK`, and [Targets.permanent]'s inverse refuses anything that is not on the battlefield —
 * deliberately, so that "destroy target creature" cannot print a script aimed at the stack.
 *
 * ### The spell type phrase is enumerated
 *
 * "Target creature or sorcery spell" is `GameObjectFilter.CreatureOrSorcery` — an ordered `Or` — and
 * "target creature spell" is one predicate, which is the same shape problem [Filters] states for its
 * own type list: English does not distinguish them except by the words. So the spell nouns are rows
 * here rather than [Filters.filter] slotted whole, and a form nobody wrote down declines. They are
 * *not* [Filters] rows because the noun ends in "spell" and the zone is part of the requirement
 * rather than of the filter — a permanent noun in this position would build a battlefield target.
 */
object Stack {

    private val spellFilter: Phrase<TargetFilter> = oneOf(
        "a spell on the stack",
        constant("spell", TargetFilter.SpellOnStack),
        constant("creature or sorcery spell", TargetFilter.CreatureOrSorcerySpellOnStack),
        constant("creature spell", TargetFilter(com.wingedsheep.sdk.scripting.GameObjectFilter.Creature, zone = Zone.STACK)),
        // The two-type nouns come before their one-type prefixes: "instant or sorcery spell" starts
        // with the same word as "instant spell", and `oneOf` commits to the first row that reads.
        constant("instant or sorcery spell", TargetFilter.InstantOrSorcerySpellOnStack),
        constant("instant spell", TargetFilter.InstantSpellOnStack),
        constant("sorcery spell", TargetFilter.SorcerySpellOnStack),
        constant("noncreature spell", TargetFilter.NoncreatureSpellOnStack),
    )

    /** "Counter target creature or sorcery spell." — Mystic Denial. */
    private val counter: Phrase<CardScript> = run {
        fun scriptFor(filter: TargetFilter) = CardScript(
            spellEffect = CounterEffect(),
            targetRequirements = listOf(TargetObject(filter = filter, id = Targets.SLOT)),
        )
        phrase("counter target {filter}", name = "counter a spell") {
            slot("filter", spellFilter)
            build { scriptFor(it.value("filter")) }
            match { script ->
                val requirement = script.targetRequirements.singleOrNull()
                    as? com.wingedsheep.sdk.scripting.targets.TargetObject ?: return@match null
                if (script != scriptFor(requirement.filter)) return@match null
                bind("filter" to requirement.filter)
            }
        }
    }

    /**
     * "Counter target spell unless its controller pays {2}." — Mana Leak, Lose Focus, Wizard Replica —
     * and its two printed variations: an `{X}` tax (Spectral Denial), and the Syncopate rider "If that
     * spell is countered this way, exile it instead of putting it into its owner's graveyard."
     *
     * The tax lands on the spelling the hand-written cards use for it, and they use two, split on a
     * property of the model rather than by habit. A fixed tax with no rider is a mana cost,
     * `Effects.CounterUnlessPays("{2}")`; `{X}` is the spell's own X (CR 107.3a) and is a *number*, so
     * it is `CounterUnlessDynamicPays(xValue())`; and the exile rider exists only on the dynamic
     * facade, so a riding fixed tax is `CounterUnlessDynamicPays(fixed(n), exileOnCounter = true)` —
     * No More Lies. A fixed tax spelled `CounterUnlessDynamicPays(fixed(n))` *without* the rider is
     * the minority spelling of the first case (Reasonable Doubt) and is not read back. Any other `{X}`
     * ("{X}, where X is ~'s power", "{1} for each …") is a different amount and declines here.
     */
    private fun counterUnlessPays(exile: Boolean): Phrase<CardScript> {
        fun effectFor(cost: ManaCost): Effect? = when {
            cost == X_TAX -> Effects.CounterUnlessDynamicPays(DynamicAmounts.xValue(), exileOnCounter = exile)
            cost.hasX -> null
            !exile -> Effects.CounterUnlessPays(cost.toString())
            else -> Primitives.genericAmount(cost)?.let {
                Effects.CounterUnlessDynamicPays(DynamicAmounts.fixed(it), exileOnCounter = true)
            }
        }
        fun scriptFor(filter: TargetFilter, cost: ManaCost) = effectFor(cost)?.let {
            CardScript(spellEffect = it, targetRequirements = listOf(TargetObject(filter = filter, id = Targets.SLOT)))
        }
        val rider = if (exile) EXILE_RIDER else ""
        return phrase(
            "counter target {filter} unless its controller pays {cost}$rider",
            name = "counter a spell unless its controller pays" + if (exile) ", exiling it" else "",
        ) {
            slot("filter", spellFilter)
            slot("cost", Primitives.manaCost)
            build { scriptFor(it.value("filter"), it.value("cost")) }
            match { script ->
                val requirement = script.targetRequirements.singleOrNull() as? TargetObject ?: return@match null
                val cost = when (val condition = (script.spellEffect as? CounterEffect)?.condition) {
                    is CounterCondition.UnlessPaysMana -> condition.cost
                    is CounterCondition.UnlessPaysDynamic -> when (val amount = condition.amount) {
                        DynamicAmount.XValue -> X_TAX
                        is DynamicAmount.Fixed -> ManaCost.parse("{${amount.amount}}")
                        else -> null
                    }
                    else -> null
                } ?: return@match null
                if (script != scriptFor(requirement.filter, cost)) return@match null
                bind("filter" to requirement.filter, "cost" to cost)
            }
        }
    }

    private val X_TAX: ManaCost = ManaCost.parse("{X}")

    private const val EXILE_RIDER =
        ". if that spell is countered this way, exile it instead of putting it into its owner's graveyard"

    val clauses: List<Phrase<CardScript>> = listOf(counter, counterUnlessPays(exile = false), counterUnlessPays(exile = true))
}
