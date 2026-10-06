package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import kotlinx.serialization.Serializable

/**
 * The mana actually spent on a payment, by type: the five colors and colorless. Recorded for an
 * activated ability's cost so the resolving ability can ask what was spent on it — Illusionary
 * Mask's "a creature card whose mana cost could be paid by some amount of, or all of, the mana you
 * spent on {X}" ([com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaCostPayableWithManaSpent]).
 */
@Serializable
data class SpentMana(
    val white: Int = 0,
    val blue: Int = 0,
    val black: Int = 0,
    val red: Int = 0,
    val green: Int = 0,
    val colorless: Int = 0,
) {
    val total: Int get() = white + blue + black + red + green + colorless

    operator fun plus(other: SpentMana) = SpentMana(
        white + other.white, blue + other.blue, black + other.black,
        red + other.red, green + other.green, colorless + other.colorless,
    )

    fun plus(color: Color?, amount: Int): SpentMana = when (color) {
        Color.WHITE -> copy(white = white + amount)
        Color.BLUE -> copy(blue = blue + amount)
        Color.BLACK -> copy(black = black + amount)
        Color.RED -> copy(red = red + amount)
        Color.GREEN -> copy(green = green + amount)
        null -> copy(colorless = colorless + amount)
    }

    /**
     * Whether some amount of, or all of, this mana could pay [cost]: an exact allocation over these
     * units, so hybrid pips take whichever half is left, generic takes anything, {C} only colorless
     * and {X} counts as 0. A {S} pip finds no snow mana here and can't be paid.
     */
    fun couldPay(cost: ManaCost): Boolean =
        ManaPool(white, blue, black, red, green, colorless).allocateFloating(cost, null) != null

    companion object {
        /** What was spent between two pool states, counting restricted entries by their color. */
        fun between(before: ManaPool, after: ManaPool): SpentMana {
            var spent = SpentMana(
                white = (before.white - after.white).coerceAtLeast(0),
                blue = (before.blue - after.blue).coerceAtLeast(0),
                black = (before.black - after.black).coerceAtLeast(0),
                red = (before.red - after.red).coerceAtLeast(0),
                green = (before.green - after.green).coerceAtLeast(0),
                colorless = (before.colorless - after.colorless).coerceAtLeast(0),
            )
            val remaining = after.restrictedMana.toMutableList()
            for (entry in before.restrictedMana) {
                if (!remaining.remove(entry)) spent = spent.plus(entry.color, 1)
            }
            return spent
        }
    }
}
