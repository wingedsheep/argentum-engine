package com.wingedsheep.ai.engine

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.costs.CostAtom

/**
 * "Can this deck pay for what the card *does*, not just for casting it?" — the deck-building reading
 * of a card's activated-ability mana.
 *
 * A castable cost is half the question. The Wandering Minstrel ({G}{U}) is on-colour in a G/U deck,
 * but its "{3}{W}{U}{B}{R}{G}: other creatures you control get +X/+X" is the reason it rates well,
 * and a G/U deck off basics can never activate it. Every activated ability's colored mana must be
 * payable from the deck's colours, or from colours its manabase fixes ([FIXING_SOURCES] or more
 * lands that make them) — otherwise the card is off-colour, whatever its cast cost says.
 *
 * Read off the card's structure: the mana atoms in each ability's [AbilityCost] on either face. A
 * plain colored pip needs its colour; a hybrid needs either half; a Phyrexian pip (2 life) and a
 * `{2/C}` pip (two generic) are always payable. A `{C}` pip needs a colorless source (CR 107.4c) —
 * Eldrazi Displacer's `{2}{C}` blink is dead in a deck without Wastes — so it is payable only when
 * the caller says the deck has them. Generic and `{X}` pips are left to the cast-cost check.
 */
object ActivationMana {

    /**
     * Lands making a colour a deck needs before that colour counts as fixed for an activation: the
     * sources a light splash runs, enough to have it by the mid-game when activations matter.
     */
    const val FIXING_SOURCES = 3

    /** The mana parts of every activated ability on [card] and its back face. */
    fun costs(card: CardDefinition): List<ManaCost> =
        (listOf(card) + listOfNotNull(card.backFace))
            .flatMap { it.script.activatedAbilities }
            .flatMap { manaIn(it.cost) }
            .filter { it.symbols.isNotEmpty() }

    /**
     * Whether a deck making [colors] — and, with [colorless], colorless mana from its pool's
     * colorless lands — can pay every activated ability's colored and `{C}` mana on [card].
     */
    fun payable(card: CardDefinition, colors: Set<Color>, colorless: Boolean = false): Boolean =
        costs(card).all { payable(it, colors, colorless) }

    /** Whether [colors] (and [colorless] sources) can pay the colored and `{C}` pips of one activation [cost]. */
    fun payable(cost: ManaCost, colors: Set<Color>, colorless: Boolean = false): Boolean = cost.symbols.all { symbol ->
        when (symbol) {
            is ManaSymbol.Phyrexian, is ManaSymbol.HybridPhyrexian, is ManaSymbol.MonocolorHybrid -> true
            is ManaSymbol.Colorless -> colorless
            else -> symbol.colors.isEmpty() || symbol.colors.any { it in colors }
        }
    }

    private fun manaIn(cost: AbilityCost): List<ManaCost> = when (cost) {
        is AbilityCost.Atom -> listOfNotNull((cost.atom as? CostAtom.Mana)?.cost)
        is AbilityCost.Composite -> cost.costs.flatMap(::manaIn)
        else -> emptyList()
    }
}
