package com.wingedsheep.sdk.scripting.values

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.GameObjectFilter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A set of colors resolved at resolution time, used by mana effects that let a player
 * (or the engine) pick a color from a constrained pool.
 *
 * `ManaColorSet` is the "color" analogue of [DynamicAmount] — pure data in the SDK,
 * resolved by the engine when an effect runs. This lets a single
 * [com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect] cover every "add one
 * mana of any color among X" pattern in the game (any color, the chosen color,
 * commander identity, colors among permanents, colors lands could produce, etc.).
 */
@Serializable
sealed interface ManaColorSet {

    /** Human-readable description of the color pool, used in oracle/effect text. */
    val description: String

    /** All five colors. The player picks any one. */
    @SerialName("ManaColorSet.AnyColor")
    @Serializable
    data object AnyColor : ManaColorSet {
        override val description: String = "any color"
    }

    /**
     * A fixed, hand-authored set of colors (e.g., `{R}{G}` for Mossfire Valley).
     * Empty set is treated as "no producible colors".
     */
    @SerialName("ManaColorSet.Specific")
    @Serializable
    data class Specific(val colors: Set<Color>) : ManaColorSet {
        override val description: String =
            if (colors.isEmpty()) "no color" else colors.joinToString(" or ") { "{${it.symbol}}" }
    }

    /**
     * The union of color identities of every commander registered to the controller
     * (Partner / Background sum their identities). Empty if the controller has no
     * commander, in which case no mana is produced. Used by Command Tower and
     * Arcane Signet.
     */
    @SerialName("ManaColorSet.CommanderIdentity")
    @Serializable
    data object CommanderIdentity : ManaColorSet {
        override val description: String = "any color in your commander's color identity"
    }

    /**
     * The union of colors of permanents matching [filter] (resolved via projected
     * state — type/color-changing effects are honored). Used by Mox Amber.
     */
    @SerialName("ManaColorSet.AmongPermanents")
    @Serializable
    data class AmongPermanents(val filter: GameObjectFilter) : ManaColorSet {
        override val description: String = "any color among matching permanents you control"
    }

    /**
     * The union of colors that any land in the given [scope] could produce
     * (CR 106.7 / Fellwar Stone rulings). Tapped state and unpayable activation
     * costs are ignored; colorless production is ignored. Used by Fellwar Stone
     * (OPPONENTS), Exotic Orchard (OPPONENTS), Reflecting Pool (YOU).
     */
    @SerialName("ManaColorSet.LandsCouldProduce")
    @Serializable
    data class LandsCouldProduce(val scope: LandControllerScope) : ManaColorSet {
        override val description: String = when (scope) {
            LandControllerScope.OPPONENTS -> "any color a land an opponent controls could produce"
            LandControllerScope.YOU -> "any color a land you control could produce"
            LandControllerScope.ANY -> "any color a land could produce"
        }
    }

    /**
     * The single color recorded on the source permanent's `ChosenColorComponent`
     * (set when it entered the battlefield, e.g., via `EntersWithChoice(COLOR)`).
     * If no color was chosen, no mana is produced. Used by Unchartered Haven and
     * Ashling Rekindled.
     */
    @SerialName("ManaColorSet.SourceChosenColor")
    @Serializable
    data object SourceChosenColor : ManaColorSet {
        override val description: String = "the chosen color"
    }
}

/**
 * Scope used by [ManaColorSet.LandsCouldProduce] to determine which lands' producible
 * colors are inspected.
 */
@Serializable
enum class LandControllerScope {
    /** Lands controlled by an opponent of the source's controller (Fellwar Stone, Exotic Orchard). */
    OPPONENTS,

    /** Lands controlled by the source's controller (Reflecting Pool). */
    YOU,

    /** Lands controlled by any player. */
    ANY,
}
