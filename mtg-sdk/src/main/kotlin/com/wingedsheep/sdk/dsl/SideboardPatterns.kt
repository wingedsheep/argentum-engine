package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Sideboard / "outside the game" recipes — the **wish** mechanic (Burning Wish, Living Wish,
 * Cunning Wish, Death Wish, Glittering Wish, Wish, …).
 *
 * A wish lets the controller bring a card they own from *outside the game* — modelled as the
 * private per-player [Zone.SIDEBOARD] (CR 100.4 / 400.11a) — into a zone in the game. Mechanically
 * it is the ordinary Gather → Select → Move pipeline (see `LibraryPatterns.searchLibrary`), pointed
 * at the sideboard instead of the library, with **no shuffle** (the sideboard is unordered and
 * stays outside the game) and **reveal on** (the chosen card is shown — CR 701.20 / the wish
 * cycle's "reveal that card"). The "may" of "you may choose a [type] card" is expressed by
 * `ChooseUpTo(1)`: declining or having no legal choice simply moves nothing.
 *
 * The varying axis across the whole wish cycle is the [filter] (Burning Wish → sorcery, Cunning
 * Wish → instant, Living Wish → creature or land, Death Wish / Wish → any); the destination is
 * [SearchDestination.HAND] for every printed wish, but is parameterized for the rare future case
 * (e.g. a Karn-style "from outside the game" that goes elsewhere).
 *
 * Reached through the single index as `Patterns.Sideboard.wish(...)`.
 */
object SideboardPatterns {

    /**
     * "You may choose a card you own from outside the game [matching [filter]], reveal it, and put
     * it into your hand." Gathers the controller's [Zone.SIDEBOARD], lets them choose up to [count]
     * (default 1) matching cards, reveals the choice, and moves it to [destination] (default hand).
     *
     * No shuffle — the sideboard is not a library. [revealed] defaults on, per the wish cycle's
     * "reveal that card" clause and CR 701.20 (Reveal); pass `revealed = false` for the cards that simply
     * "put a card you own from outside the game into your hand" with no reveal (North Wind Avatar).
     *
     * [optional] carries the printed "you may". It defaults on because every card in the wish cycle
     * proper is optional, and turns the selection into `ChooseExactly(count)` when off — for the
     * wishes worded as a plain instruction (Ring of Ma'rûf: "instead put a card you own from
     * outside the game into your hand"), which the controller must obey if able. Either way an
     * empty or unmatched sideboard simply moves nothing: `ChooseExactly` auto-selects the empty
     * set rather than stalling on an impossible choice (CR 609.3 — do as much as possible).
     */
    fun wish(
        filter: GameObjectFilter = GameObjectFilter.Any,
        count: DynamicAmount = DynamicAmount.Fixed(1),
        destination: SearchDestination = SearchDestination.HAND,
        storeAs: String = "wishable",
        revealed: Boolean = true,
        optional: Boolean = true,
    ): CompositeEffect {
        val (zone, placement) = when (destination) {
            SearchDestination.HAND -> Zone.HAND to ZonePlacement.Default
            SearchDestination.BATTLEFIELD -> Zone.BATTLEFIELD to ZonePlacement.Default
            SearchDestination.GRAVEYARD -> Zone.GRAVEYARD to ZonePlacement.Default
            SearchDestination.TOP_OF_LIBRARY -> Zone.LIBRARY to ZonePlacement.Top
        }
        val selected = "${storeAs}Found"
        return CompositeEffect(
            listOf<Effect>(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.SIDEBOARD, Player.You, filter),
                    storeAs = storeAs,
                ),
                SelectFromCollectionEffect(
                    from = storeAs,
                    selection = if (optional) SelectionMode.ChooseUpTo(count) else SelectionMode.ChooseExactly(count),
                    storeSelected = selected,
                ),
                MoveCollectionEffect(
                    from = selected,
                    destination = CardDestination.ToZone(zone, Player.You, placement),
                    revealed = revealed,
                ),
            )
        )
    }

    /** Convenience overload for the common fixed-count wish. */
    fun wish(
        filter: GameObjectFilter,
        count: Int,
        destination: SearchDestination = SearchDestination.HAND,
        revealed: Boolean = true,
        optional: Boolean = true,
    ): CompositeEffect =
        wish(filter, DynamicAmount.Fixed(count), destination, revealed = revealed, optional = optional)
}
