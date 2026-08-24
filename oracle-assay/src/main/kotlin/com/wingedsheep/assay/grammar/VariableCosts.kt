package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.normalize.Normalizer
import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.constant
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PermanentCostAction
import com.wingedsheep.sdk.scripting.costs.VariableCostMeasure
import com.wingedsheep.sdk.dsl.Costs as SdkCosts

/**
 * The count a **cost** announces — "sacrifice **any number of** creatures", "exile **one or more**
 * other artifacts you control with total mana value X", "tap **any number of** untapped creatures
 * you control other than ~ with total power 10 or greater".
 *
 * ### Why this is a family and not a row of [Cardinals]
 *
 * "any number of" looks like a word that ought to slot wherever a number word slots, and it is the
 * one thing it must never be. [Cardinals.word] is a `Phrase<Int>` over the numbers a card *fixes*;
 * a count the payer chooses is a different **value** in every position the SDK offers it —
 * `TargetRequirement.unlimited` in [Targets.quantifiers], `SelectionMode.ChooseAnyNumber` in a
 * pipeline, `SacrificeEffect.any` in an effect, and here [CostAtom.VariablePermanents], a separate
 * cost atom from [CostAtom.Sacrifice] with a separate meaning (CR 601.2b: the number is announced
 * as the ability is activated and *is* the ability's X). Slotting the phrase into the counted rules
 * would have printed one model out of two rules; it is a family over its own type instead, and the
 * word "any number of" is a row inside it.
 *
 * ### The type's three axes are the family's three axes
 *
 * `CostAtom.VariablePermanents`' own KDoc calls `action`, `xMeasure` and `minMeasure` "three
 * orthogonal axes [that] cover the printed shapes", and the grammar referenced the type nowhere at
 * all — the frozen-facade finding a sixth time, in its strongest form. So this file is the type's
 * product rather than a rule per printed sentence, and the split between what is a **row** and what
 * is a **slot** is the one [Grammar]'s entry band states: split the axes by how Oracle spells them.
 *
 *  - **[action] is a row**, because it is not one word. Each of the three spells a whole frame:
 *    a sacrifice names no controller ("sacrifice any number of creatures" — CR 701.17a lets you
 *    sacrifice only what you control, so the clause would be redundant), an exile and a tap both
 *    name one, and only a tap says "untapped" (CR 701.26a — a tapped permanent cannot pay). A slot
 *    holding the verb alone would have had to leave those three words to a template that cannot
 *    see which verb filled it.
 *  - **[minCount] is a slot**, because it *is* one word position and the noun beside it does not
 *    change: "any number of creatures" and "one or more creatures" agree in number, which is
 *    exactly the test [Targets.quantifiers] fails and the reason that table is a table.
 *  - **[measures] is a layer**, one row per printed clause, each owning `xMeasure` and `minMeasure`
 *    together. They are one axis and not two because the value space is not their product: an
 *    absent clause is `COUNT` with no floor, and a floor is only ever printed for a measure that
 *    names itself. Splitting them would make `TOTAL_POWER` with no floor spellable, which is a
 *    value no card prints and this rule would then print bare — a second printer for the bare form.
 *
 * ### `excludeSelf` is spelled in two places, and that is the action's business
 *
 * Oracle writes the exclusion as "one or more **other** artifacts you control" in front of the noun
 * and as "untapped creatures you control **other than ~**" behind it. Same field, two positions,
 * decided by the verb — so it is part of [Frame] rather than a layer of its own. A shared "other"
 * layer would have printed the wrong one for one of the two.
 *
 * ### What declines, and why it is the honest answer
 *
 * The corpus prints three more variable-count costs this type cannot hold, and each is reported
 * rather than approximated into it:
 *
 *  - **"exile any number of cards from your graveyard"** (Painbringer) and its filtered sibling
 *    "…historic cards … with total mana value 30 or greater" (The Capitoline Triad).
 *    [CostAtom.ExileFrom] takes an `Int`, and [CostAtom.CollectEvidence] — the one atom that *is*
 *    "exile any number of cards from your graveyard with total mana value N or greater" — carries
 *    no filter and no other measure. A graveyard is not the battlefield, so this type is not the
 *    answer either.
 *  - **"exile any number of red cards from your hand"** (the Adversary cycle's cost reducers).
 *    Same shape one zone further; the SDK has no variable-count hand cost at all.
 *  - **"discard any number of cards"**. [CostAtom.Discard]'s count is an `Int`.
 *
 * @see Costs for the two positions this vocabulary is lifted into.
 */
object VariableCosts {

    // -----------------------------------------------------------------------------------------
    // The count itself
    // -----------------------------------------------------------------------------------------

    /**
     * How many the payer must choose at least — Oracle's two ways of saying "you decide".
     *
     * `minCount = 0` is "any number of", which includes choosing none; `1` is "one or more", which
     * does not. The SDK's own [CostAtom.VariablePermanents.description] draws the line in the same
     * place and in the same words, which is what makes this a vocabulary rather than a convention.
     *
     * It stops there because the corpus does. "two or more" is printed once, of *drafted* cards
     * rather than of permanents, and a row for it here would round-trip against nothing.
     */
    private val minCount: Phrase<Int> = oneOf(
        "a payer-chosen count",
        constant("any number of", 0),
        constant("one or more", 1),
    )

    // -----------------------------------------------------------------------------------------
    // The measure layer
    // -----------------------------------------------------------------------------------------

    /**
     * A trailing clause that says how the chosen set is measured, and how much of it is required.
     *
     * [surface] is the whole clause including its leading space, so the bare row is the empty
     * string and no template above has to know whether it is there — the "an omissible modifier is
     * a row, not template text" rule this module's decline ranking was topped by for a year.
     */
    private class Measure(
        val surface: String,
        val measure: VariableCostMeasure,
        val floor: Int,
        val name: String,
    )

    /**
     * Every measure clause English prints behind a variable-count cost.
     *
     * `COUNT` with no floor is the bare row: an absent clause means the number chosen is itself the
     * ability's X, which is what "for each creature sacrificed this way" reads back. The two named
     * measures always carry their clause, so no two rows can print the same surface and the printed
     * form stays determined by the model.
     *
     * "with total mana value X" is the odd one and it is odd in the *model*, not the surface: it
     * names the measure and declares the resulting X in one breath (Fabrication Foundry), so its
     * floor is zero while its measure is not `COUNT`.
     */
    private val measures: List<Measure> = listOf(
        Measure("", VariableCostMeasure.COUNT, 0, "by count"),
        Measure(" with total mana value X", VariableCostMeasure.TOTAL_MANA_VALUE, 0, "for total mana value"),
    )

    /** The floored measures, whose clause carries a number the template slots. */
    private class FlooredMeasure(val surface: (String) -> String, val measure: VariableCostMeasure, val name: String)

    private val flooredMeasures: List<FlooredMeasure> = listOf(
        FlooredMeasure({ n -> " with total power $n or greater" }, VariableCostMeasure.TOTAL_POWER, "for total power"),
    )

    // -----------------------------------------------------------------------------------------
    // The action frames
    // -----------------------------------------------------------------------------------------

    /**
     * One verb, and the words the verb decides: whether the noun is qualified "untapped", whether
     * the sentence names a controller, and where the exclusion goes.
     *
     * [alsoControlled] is the second spelling of the sacrifice frame — "Sacrifice any number of
     * permanents **you control**", printed once against the bare form's many. CR 701.17a makes the
     * clause redundant, so it is the same model and therefore the same rule with an extra surface,
     * never a sibling that could drift from it.
     */
    private class Frame(
        val verb: String,
        val action: PermanentCostAction,
        val nounPrefix: String = "",
        val controlled: Boolean = true,
        val excludedBefore: Boolean = true,
        val alsoControlled: Boolean = false,
    ) {
        /** The noun phrase's surroundings, with the exclusion in whichever position this verb uses. */
        fun frame(excludeSelf: Boolean, controlled: Boolean): Pair<String, String> {
            val before = buildString {
                if (excludeSelf && excludedBefore) append("other ")
                append(nounPrefix)
            }
            val after = buildString {
                if (controlled) append(" you control")
                if (excludeSelf && !excludedBefore) append(" other than ${Normalizer.SELF}")
            }
            return before to after
        }
    }

    private val frames: List<Frame> = listOf(
        Frame("sacrifice", PermanentCostAction.SACRIFICE, controlled = false, alsoControlled = true),
        Frame("exile", PermanentCostAction.EXILE),
        Frame("tap", PermanentCostAction.TAP, nounPrefix = "untapped ", excludedBefore = false),
    )

    // -----------------------------------------------------------------------------------------
    // The product
    // -----------------------------------------------------------------------------------------

    private fun atomOf(cost: AbilityCost): CostAtom =
        requireNotNull((cost as? AbilityCost.Atom)?.atom) { "not an atom cost: $cost" }

    private fun build(
        frame: Frame,
        filter: GameObjectFilter,
        minCount: Int,
        excludeSelf: Boolean,
        measure: VariableCostMeasure,
        floor: Int,
    ): CostAtom = atomOf(
        when (frame.action) {
            PermanentCostAction.SACRIFICE ->
                SdkCosts.SacrificePermanents(filter, minCount, excludeSelf, measure, floor)

            PermanentCostAction.EXILE ->
                SdkCosts.ExilePermanents(filter, minCount, excludeSelf, measure, floor)

            PermanentCostAction.TAP ->
                SdkCosts.TapPermanentsVariable(filter, minCount, excludeSelf, measure, floor)
        }
    )

    /** One row of the product: a verb, an exclusion, a measure, and the count slotted inside it. */
    private fun rule(
        lead: (String) -> String,
        frame: Frame,
        excludeSelf: Boolean,
        measure: Measure,
    ): Phrase<CostAtom> {
        val (before, after) = frame.frame(excludeSelf, frame.controlled)
        val exclusion = if (excludeSelf) " excluding the source" else ""
        return phrase(
            "${lead(frame.verb)} {n} $before{filter}$after${measure.surface}",
            name = "${frame.verb} a chosen number of permanents ${measure.name}$exclusion",
        ) {
            slot("n", minCount)
            slot("filter", Filters.pluralSubject)
            if (frame.alsoControlled) {
                val (also, alsoAfter) = frame.frame(excludeSelf, controlled = true)
                alsoSpelled(
                    "${lead(frame.verb)} {n} $also{filter}$alsoAfter${measure.surface}",
                    name = "${frame.verb} a chosen number of permanents you control ${measure.name}$exclusion",
                )
            }
            build {
                build(frame, it.value("filter"), it.int("n"), excludeSelf, measure.measure, measure.floor)
            }
            match { atom ->
                val variable = atom as? CostAtom.VariablePermanents ?: return@match null
                if (variable.action != frame.action) return@match null
                if (variable.excludeSelf != excludeSelf) return@match null
                if (variable.xMeasure != measure.measure || variable.minMeasure != measure.floor) return@match null
                val rebuilt = build(
                    frame, variable.filter, variable.minCount, excludeSelf, measure.measure, measure.floor,
                )
                if (atom != rebuilt) return@match null
                bind("n" to variable.minCount, "filter" to variable.filter)
            }
        }
    }

    /** The floored sibling, whose measure clause carries a number of its own. */
    private fun flooredRule(
        lead: (String) -> String,
        frame: Frame,
        excludeSelf: Boolean,
        measure: FlooredMeasure,
    ): Phrase<CostAtom> {
        val (before, after) = frame.frame(excludeSelf, frame.controlled)
        val exclusion = if (excludeSelf) " excluding the source" else ""
        return phrase(
            "${lead(frame.verb)} {n} $before{filter}$after${measure.surface("{floor}")}",
            name = "${frame.verb} a chosen number of permanents ${measure.name}$exclusion",
        ) {
            slot("n", minCount)
            slot("filter", Filters.pluralSubject)
            slot("floor", Primitives.cardinal)
            build {
                val floor = it.int("floor")
                if (floor <= 0) return@build null
                build(frame, it.value("filter"), it.int("n"), excludeSelf, measure.measure, floor)
            }
            match { atom ->
                val variable = atom as? CostAtom.VariablePermanents ?: return@match null
                if (variable.action != frame.action) return@match null
                if (variable.excludeSelf != excludeSelf) return@match null
                if (variable.xMeasure != measure.measure || variable.minMeasure <= 0) return@match null
                val rebuilt = build(
                    frame, variable.filter, variable.minCount, excludeSelf, measure.measure, variable.minMeasure,
                )
                if (atom != rebuilt) return@match null
                bind("n" to variable.minCount, "filter" to variable.filter, "floor" to variable.minMeasure)
            }
        }
    }

    /**
     * The whole family in one capitalization — [Costs.vocabulary]'s argument, for its reason.
     *
     * Returned as a list rather than as one `oneOf` so the rows land in the cost vocabulary's own
     * alternation, where the ambiguity gate compares them against every other atom rather than only
     * against each other.
     */
    fun rows(lead: (String) -> String): List<Phrase<CostAtom>> = buildList {
        for (frame in frames) {
            for (excludeSelf in listOf(false, true)) {
                for (measure in measures) add(rule(lead, frame, excludeSelf, measure))
                for (measure in flooredMeasures) {
                    add(flooredRule(lead, frame, excludeSelf, measure))
                }
            }
        }
    }

    /**
     * The same vocabulary as one phrase, mid-sentence — what a **payable** cost slots.
     *
     * `PayCost` is `CostAtom`'s third context ("sacrifice it unless you sacrifice any number of
     * creatures with total power 12 or greater" — Phyrexian Dreadnought), and it takes the family
     * whole for the reason [Costs] gives for taking it whole in the other two: a row added here has
     * to reach every context that can pay it, or the vocabulary is three lists again.
     */
    val payAtoms: Phrase<CostAtom> = oneOf("a chosen-count cost", rows { it })
}
