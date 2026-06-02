package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Library Effects
// =============================================================================

/**
 * Shuffle a player's library.
 * "Shuffle your library" or "Target player shuffles their library"
 */
@SerialName("ShuffleLibrary")
@Serializable
data class ShuffleLibraryEffect(
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "Shuffle your library"
        else -> "${target.description.replaceFirstChar { it.uppercase() }} shuffles their library"
    }
}

/**
 * Emit a `ScriedEvent` after a scry pipeline finishes resolving. Appended internally
 * by [com.wingedsheep.sdk.dsl.LibraryPatterns.scry] so that "Whenever you scry"
 * triggers ([com.wingedsheep.sdk.dsl.Triggers.WheneverYouScry]) fire exactly once
 * per scry, carrying the actual number of cards looked at.
 *
 * The count is the size of the named gather collection (`"scried"` by default) at
 * resolution time — i.e. the cards `GatherCardsEffect` actually pulled, which equals
 * the scry N parameter unless the library held fewer (CR 701.18a). The count can be
 * zero when the library was empty; the event still fires, because CR 701.18d triggers
 * "whenever you scry" abilities "even if some or all of those actions were impossible."
 * Suppression of a literal "scry 0" (CR 701.18b) is handled by `scry()` omitting this
 * tail entirely, not here.
 *
 * Card authors should not use this directly; it is wired into the scry primitive.
 */
@SerialName("EmitScriedEvent")
@Serializable
data class EmitScriedEventEffect(
    val gatherCollection: String = "scried"
) : Effect {
    // Intentionally blank: this is an internal pipeline tail with no player-facing text.
    override val description: String = ""
}


/**
 * Destination for searched cards.
 */
@Serializable
enum class SearchDestination(val description: String) {
    HAND("into your hand"),
    BATTLEFIELD("onto the battlefield"),
    GRAVEYARD("into your graveyard"),
    TOP_OF_LIBRARY("on top of your library")
}



/**
 * Take the top card from the source permanent's linked exile pile and put it
 * into the controller's hand. Used by Parallel Thoughts and similar cards that
 * exile a pile of cards and later retrieve from it.
 *
 * If the linked exile pile is empty, nothing happens.
 */
/**
 * Repeatedly exile cards from the top of a player's library until a card matching
 * [matchFilter] is found, then put that card into the player's hand. If the matched
 * card's mana value is at least [repeatIfManaValueAtLeast], repeat the process.
 * After the process completes, the source deals damage to the player equal to the
 * number of cards put into their hand this way (multiplied by [damagePerCard]).
 *
 * Land cards exiled this way remain in exile.
 *
 * Used for Demonlord Belzenlok and similar "exile-reveal-repeat" library effects.
 */
@SerialName("ExileFromTopRepeating")
@Serializable
data class ExileFromTopRepeatingEffect(
    val matchFilter: GameObjectFilter = GameObjectFilter.Nonland,
    val repeatIfManaValueAtLeast: Int = 4,
    val damagePerCard: Int = 1
) : Effect {
    override val description: String = buildString {
        append("Exile cards from the top of your library until you exile a ${matchFilter.description} card, ")
        append("then put that card into your hand. ")
        append("If the card's mana value is $repeatIfManaValueAtLeast or greater, repeat this process. ")
        if (damagePerCard > 0) {
            append("Deal $damagePerCard damage to you for each card put into your hand this way.")
        }
    }
}

/**
 * For each matching player, exile cards from the top of their library until
 * the total mana value of cards exiled this way for that player reaches at
 * least [threshold]. All exiled card entity IDs (across every matched player)
 * are stored in the pipeline collection [storeAs] so downstream steps — e.g.
 * [GrantMayPlayFromExileEffect] + [GrantPlayWithoutPayingCostEffect] — operate
 * on them with the outer controller preserved.
 *
 * {X} in a library card's cost contributes 0 to mana value (matching rule
 * 107.3b / Dream Harvest ruling). If a player's library is exhausted before
 * the threshold is met, all of that player's remaining library is exiled.
 *
 * Used for Dream Harvest ("Each opponent exiles cards from the top of their
 * library until they have exiled cards with total mana value 5 or greater").
 */
@SerialName("ExileLibraryUntilManaValue")
@Serializable
data class ExileLibraryUntilManaValueEffect(
    val players: Player = Player.EachOpponent,
    val threshold: DynamicAmount,
    val storeAs: String
) : Effect {
    override val description: String = buildString {
        append(players.description.replaceFirstChar { it.uppercase() })
        append(" exiles cards from the top of ")
        append(players.possessive)
        append(" library until ")
        append(if (players == Player.You) "you have" else "they have")
        append(" exiled cards with total mana value ${threshold.description} or greater this way")
    }
}

/**
 * Cascade (CR 702.85a). Resolves the cascade ability of the triggering spell:
 * 1. Exile cards from the top of the controller's library one at a time.
 * 2. Stop when a nonland card with mana value strictly less than the triggering
 *    spell's mana value is exiled. That card becomes the "cascade card".
 * 3. The controller is asked whether to cast the cascade card without paying
 *    its mana cost. On yes the cast is synthesized immediately (still during
 *    cascade resolution); on no the cascade card stays uncast.
 * 4. Every exiled card that wasn't cast is put on the bottom of the library
 *    in a random order.
 *
 * The threshold is read at execution time from the triggering spell pointed at
 * by the engine's trigger context. If the triggering entity is missing or has
 * no mana value, the cascade trigger fizzles (does nothing). If no qualifying
 * card is found (library exhausted), no may-cast decision is offered and every
 * exiled card is put on the bottom.
 */
/**
 * Atomic "cast a card from a pipeline collection without paying its mana cost". Takes a
 * stored collection (which should be bounded to 0..1 cards by an upstream
 * [SelectFromCollectionEffect] using [SelectionMode] `ChooseUpTo(1)` or `ChooseExactly(1)`)
 * and, if non-empty, casts the first card for free.
 *
 * The executor:
 * 1. Stamps the card with `PlayWithoutPayingCostComponent` and registers a fresh
 *    `MayPlayPermission` scoped to the source's controller — the card must already be in a
 *    zone where casting is technically legal (e.g. exile after a prior `MoveCollection`).
 * 2. Synthesizes a `CastSpell` action through the normal cast machinery, so target / X /
 *    mode prompts surface to the controller during the same resolution window.
 *
 * Empty collection: no-op. Cast couldn't initiate (no legal targets, etc.): no-op — the
 * card stays in its current zone for any downstream cleanup step to handle.
 *
 * Composed with `GatherCardsEffect` → `MoveCollectionEffect`(→exile) →
 * `SelectFromCollectionEffect`(`ChooseUpTo(1)`, eligibility filter, `showAllCards = true`)
 * → `MoveCollectionEffect`(remainder → library bottom, `CardOrder.Random`) → this effect,
 * this primitive expresses Sunbird's-Invocation-shaped flows without a bespoke executor.
 */
@SerialName("CastFromCollectionWithoutPayingCost")
@Serializable
data class CastFromCollectionWithoutPayingCostEffect(
    val from: String,
) : Effect {
    override val description: String =
        "Cast the $from card without paying its mana cost"
}

/**
 * Cast ANY NUMBER of cards from a named pipeline collection **during this effect's
 * resolution**, each without paying its mana cost, with timing restrictions based on card
 * type ignored (Rule 608.2 / "cast … while [the source] is resolving").
 *
 * The multi-cast sibling of [CastFromCollectionWithoutPayingCostEffect]. It loops: the
 * controller is repeatedly offered the cards still in the collection and may pick one to cast
 * for free (its targets / X / modes flow through the normal cast machinery), then is offered
 * the rest, until they decline. Because the casts happen synchronously while the source
 * resolves — exactly like Cascade — sorcery-speed timing does not apply, and no lingering
 * "you may play it later" permission is granted, so cards left uncast can't be cast later.
 *
 * Hand it the already-eligible set: filter the collection upstream (e.g. nonland +
 * `FilterCollection(ManaValueAtMost(...))`); this effect casts from whatever it's given and
 * leaves the rest in place.
 *
 * Models the "you may cast any number of spells with mana value X or less from among them
 * without paying their mana costs" clause (Kotis, the Fangkeeper; Villainous-Wealth-shaped
 * cards once migrated).
 *
 * @property from Name of the collection of already-exiled candidate cards.
 */
@SerialName("CastAnyNumberFromCollectionWithoutPayingCost")
@Serializable
data class CastAnyNumberFromCollectionWithoutPayingCostEffect(
    val from: String,
) : Effect {
    override val description: String =
        "Cast any number of the $from cards without paying their mana costs"
}

@SerialName("Cascade")
@Serializable
data object CascadeEffect : Effect {
    override val description: String =
        "Exile cards from the top of your library until you exile a nonland card " +
            "with mana value less than this spell's. You may cast it without paying its " +
            "mana cost. Put the exiled cards on the bottom in a random order."
}

