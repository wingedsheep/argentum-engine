package com.wingedsheep.mtg.sets.definitions.arn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ring of Ma'rûf
 * {5}
 * Artifact
 * {5}, {T}, Exile this artifact: The next time you would draw a card this turn, instead put a
 * card you own from outside the game into your hand.
 *
 * The odd one out of the wish family: it does not fetch on resolution, it installs a one-shot
 * draw-replacement shield. Composition, no new vocabulary:
 *  - `{5}, {T}, Exile this artifact` is `Costs.Composite(Mana, Tap, ExileSelf)` — the same
 *    exile-as-a-cost shape as Feldon's Cane. The ability resolves after the Ring is already gone.
 *  - `Effects.ReplaceNextDraw(...)` is the "Words of" cycle's shield, which Aladdin's Lamp in this
 *    same set also uses: "the next time you would draw a card this turn, [effect] instead."
 *  - The replacement is `Patterns.Sideboard.wish` over the controller's sideboard ("outside the
 *    game", CR 100.4 / 400.11a), filtered to nothing in particular — "a card you own". No reveal
 *    clause, so `revealed = false` (as North Wind Avatar), and no "may", so `optional = false`:
 *    the controller must take a card if they own one outside the game. An empty sideboard moves
 *    nothing, and the draw is still replaced.
 *
 * Per the 2007-09-16 ruling, the replacement is the whole card: if you would not draw a card
 * again this turn, the ability does nothing at all — unlike Burning Wish and friends, which put
 * the card in your hand the moment they resolve.
 */
val RingOfMaruf = card("Ring of Ma'rûf") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{5}, {T}, Exile this artifact: The next time you would draw a card this turn, " +
        "instead put a card you own from outside the game into your hand."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap, Costs.ExileSelf)
        effect = Effects.ReplaceNextDraw(
            Patterns.Sideboard.wish(GameObjectFilter.Any, revealed = false, optional = false)
        )
        description = "{5}, {T}, Exile this artifact: The next time you would draw a card this turn, " +
            "instead put a card you own from outside the game into your hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "68"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fcc1004f-7cee-420a-9f0e-2986ed3ab852.jpg?1783948377"
        ruling(
            "2007-09-16",
            "Ring of Ma'rûf works a little differently than the Wishes from others sets. Rather " +
                "than letting you simply put a card into your hand from outside the game, this " +
                "ability replaces your next draw. If you wouldn't draw a card during the rest of " +
                "the turn, the ability won't have any effect."
        )
    }
}
