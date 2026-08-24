package com.wingedsheep.assay.grammar

import com.wingedsheep.assay.syntax.Phrase
import com.wingedsheep.assay.syntax.bind
import com.wingedsheep.assay.syntax.deferred
import com.wingedsheep.assay.syntax.oneOf
import com.wingedsheep.assay.syntax.phrase
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * "creature card with **mana value 3 or less**" — the mana-value qualifier, in every shape Oracle
 * and the SDK both have one for.
 *
 * A layer of [Filters]' cascade in the sense that file's KDoc means: it owns exactly one
 * [CardPredicate] at the top of the stack, strips precisely that, and delegates the rest inwards.
 * What makes it a file of its own is that the layer is not one rule but a **product**, and the two
 * axes are not independent.
 *
 * ## The comparison's spelling is a function of the value's shape
 *
 * | | equal | at most | at least |
 * |---|---|---|---|
 * | a numeral | `mana value 3` | `mana value 3 or less` | `mana value 3 or greater` |
 * | the announced `X` | `mana value X` | `mana value X or less` | — |
 * | a clause | `mana value equal to …` | `mana value less than or equal to …` | — |
 *
 * English postfixes the comparison to a number — "3 **or less**" — and prefixes it to a phrase —
 * "**less than or equal to** the number of lands you control". Nothing about the model chooses
 * between them: `ManaValueAtMost(3)` has one printed form and `ManaValueAtMostDynamic(…)` has
 * another, and which one is legal is decided by the value standing beside it. So the comparison
 * cannot be a slot over one template — a slot would make "mana value less than or equal to 3" a
 * second spelling of the first row, which is the underdetermined printing this module refuses. It is
 * three generators over one comparison list instead, and each generator takes only the comparisons
 * its value shape has a spelling for.
 *
 * The equality row is the same observation at its limit: **equality's postfix form is the empty
 * string.** "Mana value 3" is not an abbreviation of a comparison Oracle left out; the bare numeral
 * *is* how the equality is written, which is why `""` is a row of the table ([EXACTLY]) rather than
 * a special case above it.
 *
 * ## The two empty cells are declared, not forgotten
 *
 * Both absences are the SDK's, and both are visible in the corpus:
 *
 * - **`X or greater`** — one card prints it, and there is no `ManaValueAtLeastX` for it to be. The
 *   `X` predicates are a closed pair ([CardPredicate.ManaValueEqualsX],
 *   [CardPredicate.ManaValueAtMostX]) and inventing the third here would be a model no engine path
 *   reads.
 * - **a clause with `or greater`** — no card prints it at all, so there is nothing to read and
 *   nothing to name.
 *
 * Across the whole Oracle bulk the seven filled cells cover **852 of the 856** places a mana-value
 * qualifier appears. The remaining four are "mana value 4 or 5" (Transit Mage and three siblings), a
 * *disjunction* of two equalities rather than a comparison, which is a different construct and
 * declines as one.
 *
 * ## Why the clause slot is deferred
 *
 * A qualifier can be measured by a count and a count is taken over a noun phrase, so
 * [Filters] → this → [Amounts] → [Filters] is a genuine cycle in English. The
 * [deferred] indirection is the kernel's answer to it; see its KDoc for what the alternative
 * (a second `DynamicAmount` vocabulary) would cost.
 */
object ManaValues {

    /** The head of the clause, shared by every row: the model has nowhere to keep a synonym. */
    private const val LEAD = "with mana value"

    // -----------------------------------------------------------------------------------------
    // The predicate stack, as this layer is allowed to see it
    // -----------------------------------------------------------------------------------------

    /**
     * The predicate this layer would own, if the filter's stack ends in one at all.
     *
     * [Filters]' own `stripTop` is the same operation reified on a predicate *type*, which is what
     * its layers need because each owns one class. Here the seven rows own seven classes and a row's
     * `read` is what says which — so the split is (peek, drop) rather than a typed strip.
     */
    private fun GameObjectFilter.top(): CardPredicate? = cardPredicates.lastOrNull()

    /** …and the filter without it, which is what a row hands back inwards. */
    private fun GameObjectFilter.dropTop(): GameObjectFilter =
        copy(cardPredicates = cardPredicates.dropLast(1))

    // -----------------------------------------------------------------------------------------
    // The three generators, one per value shape
    // -----------------------------------------------------------------------------------------

    /**
     * A numeral with the comparison behind it — "mana value 3", "mana value 3 or less".
     *
     * [build] goes through `ObjectFilter`'s fluent builders rather than appending a predicate
     * directly, for [Filters]' stated reason: the builders are what make "the top of the stack" the
     * well-defined thing a layer owns.
     */
    private fun fixed(
        inner: Phrase<GameObjectFilter>,
        postfix: String,
        name: String,
        build: (GameObjectFilter, Int) -> GameObjectFilter,
        read: (CardPredicate) -> Int?,
    ): Phrase<GameObjectFilter> =
        phrase("{type} $LEAD {n}$postfix", name = name) {
            slot("type", inner)
            slot("n", Primitives.cardinal)
            build { build(it.value("type"), it.int("n")) }
            match { filter ->
                val n = read(filter.top() ?: return@match null) ?: return@match null
                val rest = filter.dropTop()
                // Strip, then rebuild and compare: the two halves are the same `build` closure, so
                // a row cannot print a filter its own parse would not reproduce.
                if (build(rest, n) != filter) return@match null
                bind("type" to rest, "n" to n)
            }
        }

    /**
     * The letter `X` in the same position — "mana value X or less".
     *
     * A row rather than a value inside [fixed]'s slot, because the SDK types the two differently:
     * `ManaValueAtMost(3)` carries a number and [CardPredicate.ManaValueAtMostX] carries none, being
     * the X announced for the spell or ability this filter belongs to. That is
     * [Amounts.WHERE_X]'s lesson in a fourth position — a word cannot be a slot when the model
     * changes shape underneath it.
     *
     * `X` here is always the *announced* X and never a clause-defined one: a card that goes on to
     * say ", where X is …" (Eldritch Evolution) is naming a different value in the same letter, and
     * that clause is a family of its own. It declines on the clause rather than misreading the row.
     */
    private fun announced(
        inner: Phrase<GameObjectFilter>,
        postfix: String,
        name: String,
        build: (GameObjectFilter) -> GameObjectFilter,
    ): Phrase<GameObjectFilter> =
        phrase("{type} $LEAD X$postfix", name = name) {
            slot("type", inner)
            build { build(it.value("type")) }
            match { filter ->
                val rest = filter.dropTop().takeIf { filter.cardPredicates.isNotEmpty() } ?: return@match null
                if (build(rest) != filter) return@match null
                bind("type" to rest)
            }
        }

    /**
     * A count, with the comparison in front of it — "mana value less than or equal to the number of
     * lands you control".
     *
     * The slot is [Amounts.count], which cannot print a `Fixed` amount or
     * [DynamicAmount.XValue] — the two shapes the rows above own. So the partition between the
     * three generators is enforced by the count vocabulary itself rather than by a guard here, and a
     * hand-written `ManaValueAtMostDynamic(Fixed(3))` declines instead of coming back as a second
     * reading of "mana value 3 or less".
     */
    private fun clause(
        inner: Phrase<GameObjectFilter>,
        lead: String,
        name: String,
        build: (GameObjectFilter, DynamicAmount) -> GameObjectFilter,
        read: (CardPredicate) -> DynamicAmount?,
    ): Phrase<GameObjectFilter> =
        phrase("{type} $LEAD $lead {amount}", name = name) {
            slot("type", inner)
            slot("amount", deferred("a count") { Amounts.count })
            build { build(it.value("type"), it.value("amount")) }
            match { filter ->
                val amount = read(filter.top() ?: return@match null) ?: return@match null
                val rest = filter.dropTop()
                if (build(rest, amount) != filter) return@match null
                bind("type" to rest, "amount" to amount)
            }
        }

    // -----------------------------------------------------------------------------------------
    // The table
    // -----------------------------------------------------------------------------------------

    /** Equality's postfix form, kept as a name because `""` in a template reads as a typo. */
    private const val EXACTLY = ""

    /**
     * The whole layer, over one inner noun phrase.
     *
     * [suffix] distinguishes the rule names between the positions the cascade is instantiated in —
     * permanent, card, plural — exactly as [Filters]' other layers take one.
     */
    fun layer(inner: Phrase<GameObjectFilter>, suffix: String): Phrase<GameObjectFilter> = oneOf(
        "a mana value$suffix",
        fixed(inner, EXACTLY, "a mana value$suffix", GameObjectFilter::manaValue) {
            (it as? CardPredicate.ManaValueEquals)?.value
        },
        fixed(inner, " or less", "a mana value at most$suffix", GameObjectFilter::manaValueAtMost) {
            (it as? CardPredicate.ManaValueAtMost)?.max
        },
        fixed(inner, " or greater", "a mana value at least$suffix", GameObjectFilter::manaValueAtLeast) {
            (it as? CardPredicate.ManaValueAtLeast)?.min
        },
        announced(inner, EXACTLY, "the announced mana value$suffix", GameObjectFilter::manaValueEqualsX),
        announced(inner, " or less", "the announced mana value at most$suffix", GameObjectFilter::manaValueAtMostX),
        clause(inner, "equal to", "a counted mana value$suffix", GameObjectFilter::manaValueEqualsDynamic) {
            (it as? CardPredicate.ManaValueEqualsDynamic)?.amount
        },
        clause(
            inner,
            "less than or equal to",
            "a counted mana value at most$suffix",
            GameObjectFilter::manaValueAtMostDynamic,
        ) { (it as? CardPredicate.ManaValueAtMostDynamic)?.amount },
    )
}
