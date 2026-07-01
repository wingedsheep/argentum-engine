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
 * Emit a `SurveiledEvent` after a surveil pipeline finishes resolving — the surveil twin of
 * [EmitScriedEventEffect]. Appended internally by [com.wingedsheep.sdk.dsl.LibraryPatterns.surveil]
 * so "Whenever you surveil" / "Whenever you scry or surveil" triggers
 * ([com.wingedsheep.sdk.dsl.Triggers.WheneverYouSurveil],
 * [com.wingedsheep.sdk.dsl.Triggers.WheneverYouScryOrSurveil]) fire exactly once per surveil,
 * carrying the actual number of cards looked at.
 *
 * The count is the size of the named gather collection (`"surveiled"` by default) at resolution
 * time — the cards `GatherCardsEffect` actually pulled, which equals the surveil N parameter unless
 * the library held fewer (CR 701.42a). The count can be zero when the library was empty; the event
 * still fires, because CR 701.42d triggers "whenever you surveil" abilities "even if some or all of
 * those actions were impossible." Suppression of a literal "surveil 0" (CR 701.42c) is handled by
 * `surveil()` omitting this tail entirely, not here.
 *
 * Card authors should not use this directly; it is wired into the surveil primitive.
 */
@SerialName("EmitSurveiledEvent")
@Serializable
data class EmitSurveiledEventEffect(
    val gatherCollection: String = "surveiled"
) : Effect {
    // Intentionally blank: this is an internal pipeline tail with no player-facing text.
    override val description: String = ""
}

/**
 * Emit a `ManifestedDreadEvent` after a manifest-dread pipeline finishes resolving — the
 * manifest-dread twin of [EmitScriedEventEffect]. Appended internally by
 * [com.wingedsheep.sdk.dsl.LibraryPatterns.manifestDread] so "Whenever you manifest dread"
 * triggers ([com.wingedsheep.sdk.dsl.Triggers.WheneverYouManifestDread]) fire exactly once per
 * manifest-dread (CR 701.60), after the chosen card has been manifested and the other put into
 * the graveyard.
 *
 * The executor reads [graveyardCollection] — the named pipeline collection of cards put into the
 * graveyard this way — and carries those entity ids on the event so the resolving trigger can
 * pull "a card you put into your graveyard this way" back out (Paranormal Analyst). The collection
 * is empty when the library held fewer than two cards; the event still fires (CR 701.60b), the
 * payoff simply finds nothing to return.
 *
 * Card authors should not use this directly; it is wired into the manifest-dread primitive.
 */
@SerialName("EmitManifestedDreadEvent")
@Serializable
data class EmitManifestedDreadEventEffect(
    val graveyardCollection: String = "manifestDreadGraveyard"
) : Effect {
    // Intentionally blank: this is an internal pipeline tail with no player-facing text.
    override val description: String = ""
}

/**
 * Emit a `LibrarySearchedEvent` after a library-search pipeline finishes resolving — the search
 * twin of [EmitScriedEventEffect]. Appended internally by
 * [com.wingedsheep.sdk.dsl.LibraryPatterns.searchLibrary] /
 * [com.wingedsheep.sdk.dsl.LibraryPatterns.searchMultipleZones] /
 * [com.wingedsheep.sdk.dsl.LibraryPatterns.eachPlayerSearchesLibrary] so "Whenever a player
 * searches their library" triggers ([com.wingedsheep.sdk.dsl.Triggers.WheneverYouSearchYourLibrary],
 * [com.wingedsheep.sdk.dsl.Triggers.WheneverAnOpponentSearchesTheirLibrary]) fire exactly once per
 * search (CR 701.23), after the found cards have moved and the library has shuffled.
 *
 * The searching player is the effect's controller at resolution time — for a per-player
 * `ForEachPlayer` search (each player searches their own library) that controller is rebound to
 * each iterated player, so the event carries the correct searcher. Searching is the act of looking
 * through the zone (CR 701.23a) and finding a card is not required (CR 701.23b), so the event still
 * fires when no card was found.
 *
 * Card authors should not use this directly; it is wired into the search primitives.
 */
@SerialName("EmitLibrarySearchedEvent")
@Serializable
data object EmitLibrarySearchedEventEffect : Effect {
    // Intentionally blank: this is an internal pipeline tail with no player-facing text.
    override val description: String = ""
}


/**
 * "Scry [count]" (CR 701.18) as a single compact node.
 *
 * This is a *macro effect*: a serializable marker that, at execution time, expands into the
 * shared Gather → Select → Move → Move → emit pipeline built by
 * [com.wingedsheep.sdk.dsl.LibraryPatterns.scryPipeline]. The expansion reuses every atomic
 * library executor (and its choose-pause / continuation plumbing), so this node adds no new
 * gather/select/move logic — it only collapses the representation.
 *
 * Why a marker instead of the unrolled composite: a card whose effect *is* "scry 2" should
 * serialize as `{"type":"Scry","count":2}` and read in the SDK as `Effects.Scry(2)`, rather than
 * the full five-step pipeline. That keeps the `CardDefinitionSnapshotTest` goldens one line per
 * scry and stops them churning whenever the shared pipeline internals change.
 *
 * Use the [com.wingedsheep.sdk.dsl.Effects.Scry] / [com.wingedsheep.sdk.dsl.LibraryPatterns.scry]
 * facade, not this constructor.
 */
@SerialName("Scry")
@Serializable
data class ScryEffect(val count: Int) : Effect {
    override val description: String = "Scry $count"
}

/**
 * "Surveil [count]" (CR 701.42) as a single compact node — the surveil twin of [ScryEffect].
 *
 * Expands at execution time into the shared pipeline built by
 * [com.wingedsheep.sdk.dsl.LibraryPatterns.surveilPipeline]; see [ScryEffect] for the macro-effect
 * rationale. Use the [com.wingedsheep.sdk.dsl.Effects.Surveil] /
 * [com.wingedsheep.sdk.dsl.LibraryPatterns.surveil] facade, not this constructor.
 */
@SerialName("Surveil")
@Serializable
data class SurveilEffect(val count: Int) : Effect {
    override val description: String = "Surveil $count"
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
 *
 * **Paying the mana cost.** Set [payManaCost] to `true` for the "you may cast it" wording that
 * *doesn't* say "without paying its mana cost" (Kaervek, the Punisher — "copy it. You may cast
 * the copy."). The executor then grants only the [MayPlayPermission] that makes the card
 * castable from its current zone and lets `CastSpellHandler` charge the normal cost (so an {X}
 * spell prompts for X, Rule 601.2b). With the default `false` the card is also stamped
 * `PlayWithoutPayingCostComponent` and cast for free.
 *
 * **Gating a follow-up on whether the cast happened.** Set [storeCastTo] to publish the cast
 * card's id into that pipeline collection once the cast successfully initiates (synchronously or
 * after a target / X pause). Pair it with `IfYouDoEffect(this, then, SuccessCriterion
 * .CollectionNonEmpty(storeCastTo))` for "you may cast … . If you do, [then]" — the follow-up is
 * skipped when the player declines or the cast can't be paid for (Kaervek's "If you do, you lose
 * 2 life"). The collection is left empty when nothing was cast.
 */
@SerialName("CastFromCollectionWithoutPayingCost")
@Serializable
data class CastFromCollectionWithoutPayingCostEffect(
    val from: String,
    /** When true, the controller pays the spell's normal mana cost instead of casting for free. */
    val payManaCost: Boolean = false,
    /** When set, the cast card's id is published to this pipeline collection on a successful cast. */
    val storeCastTo: String? = null,
) : Effect {
    override val description: String =
        if (payManaCost) "Cast that card" else "Cast that card without paying its mana cost"
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
 * **Paying the mana cost.** Set [payManaCost] to `true` for the "you may cast any number of [them]"
 * wording that *doesn't* say "without paying their mana costs" (The Tale of Tamiyo IV — "Copy them.
 * You may cast any number of the copies."). Each chosen card is then cast paying its normal cost
 * (an {X} spell prompts for X, Rule 601.2b); with the default `false` each is cast for free.
 *
 * @property from Name of the collection of already-exiled candidate cards.
 * @property payManaCost When true, each chosen card is cast paying its normal mana cost.
 */
@SerialName("CastAnyNumberFromCollectionWithoutPayingCost")
@Serializable
data class CastAnyNumberFromCollectionWithoutPayingCostEffect(
    val from: String,
    val payManaCost: Boolean = false,
) : Effect {
    override val description: String =
        if (payManaCost) "Cast any number of those cards"
        else "Cast any number of those cards without paying their mana costs"
}

@SerialName("Cascade")
@Serializable
data object CascadeEffect : Effect {
    override val description: String =
        "Exile cards from the top of your library until you exile a nonland card " +
            "with mana value less than this spell's. You may cast it without paying its " +
            "mana cost. Put the exiled cards on the bottom in a random order."
}

