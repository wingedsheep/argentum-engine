package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Roiling Regrowth — Zendikar Rising #201
 * {2}{G} · Instant
 *
 * Sacrifice a land. Search your library for up to two basic land cards, put them onto the
 * battlefield tapped, then shuffle.
 *
 * The sacrifice is part of the resolution, not a cost — the same shape as Cycle of Renewal and
 * Entish Restoration: `Effects.SacrificeOwn(Land)` then an up-to-two basic-land search to the
 * battlefield tapped.
 */
val RoilingRegrowth = card("Roiling Regrowth") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Sacrifice a land. Search your library for up to two basic land cards, put them onto the " +
        "battlefield tapped, then shuffle."

    spell {
        effect = Effects.SacrificeOwn(GameObjectFilter.Land) then
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 2,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Jonas De Ro"
        flavorText = "\"Roots find their way through rock, and even mountains must give way to new growth.\"\n—Nissa Revane"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa259096-b24f-414c-af59-301bed4b4627.jpg?1783929332"
    }
}
