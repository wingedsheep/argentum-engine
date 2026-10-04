package com.wingedsheep.sdk.scripting.values

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
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
     * The union of colors among the cards in your graveyard matching [filter] (read from each card's
     * base colors — graveyard cards aren't projected). Used by The Grey Havens: "Add one mana of any
     * color among legendary creature cards in your graveyard."
     */
    @SerialName("ManaColorSet.AmongCardsInGraveyard")
    @Serializable
    data class AmongCardsInGraveyard(val filter: GameObjectFilter) : ManaColorSet {
        override val description: String = "any color among matching cards in your graveyard"
    }

    /**
     * The union of colors that any land in the given [scope] could produce
     * (CR 106.7 / Fellwar Stone rulings). Tapped state and unpayable activation
     * costs are ignored; colorless production is ignored. Used by Fellwar Stone
     * (OPPONENTS), Exotic Orchard (OPPONENTS), Reflecting Pool (YOU). "Any *type*" adds a
     * second `{C}` ability gated on `CardPredicate.CouldProduceColorlessMana` (Naga Vitalist).
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
     * The union of colors among the cards currently exiled *with* the source permanent — the
     * cards recorded in its `LinkedExileComponent` (set by `MoveToZoneEffect(linkToSource = true)`)
     * that are still in the exile zone. Colors are read from each exiled card's base colors
     * (exile-zone cards aren't projected). Empty if the source exiled no cards, they've all since
     * left exile, or every exiled card is colorless — in which case no mana is produced. Used by
     * Pit of Offerings: "Add one mana of any of the exiled cards' colors."
     */
    @SerialName("ManaColorSet.AmongLinkedExiledCards")
    @Serializable
    data object AmongLinkedExiledCards : ManaColorSet {
        override val description: String = "any of the exiled cards' colors"
    }

    /**
     * The colors of one object — [entity] resolved against the running effect: a pipeline-gathered
     * card (`EffectTarget.PipelineTarget`), a target, the source (`EffectTarget.Self`). A battlefield
     * permanent's colors are read from projected state; any other zone uses the card's own colors.
     * A colorless object, or one that can't be resolved, produces no mana. Outside an effect (the mana
     * solver asking what a mana ability could make) only `Self` resolves.
     *
     * "Add three mana in any combination of its colors" (Omnath, Locus of All) is
     * `Effects.Repeat(3, AddManaOfChoice(ColorsOf(card)))` — each unit picks its own colour.
     */
    @SerialName("ManaColorSet.ColorsOf")
    @Serializable
    data class ColorsOf(val entity: EffectTarget) : ManaColorSet {
        override val description: String = "any of ${entity.description}'s colors"
    }

    /**
     * The single color recorded on the source permanent's `CastChoicesComponent`
     * (set when it entered the battlefield, e.g., via `EntersWithChoice(COLOR)`).
     * If no color was chosen, no mana is produced. Used by Unchartered Haven and
     * Ashling Rekindled.
     */
    @SerialName("ManaColorSet.SourceChosenColor")
    @Serializable
    data object SourceChosenColor : ManaColorSet {
        override val description: String = "the chosen color"
    }

    /**
     * The union of several pools — the player picks one color from any of them. Models a mana
     * ability that names a fixed color *or* a looked-up one: the Thriving lands' "Add {R} or one
     * mana of the chosen color" is `Union(listOf(Specific(setOf(RED)), SourceChosenColor))`, which
     * produces only {R} until a color has been chosen. Duplicates across members collapse (a set).
     */
    @SerialName("ManaColorSet.Union")
    @Serializable
    data class Union(val members: List<ManaColorSet>) : ManaColorSet {
        init {
            require(members.size >= 2) { "ManaColorSet.Union needs at least two members" }
        }

        override val description: String = members.joinToString(" or ") { it.description }
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
