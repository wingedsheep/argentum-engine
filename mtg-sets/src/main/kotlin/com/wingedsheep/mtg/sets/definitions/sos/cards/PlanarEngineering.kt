package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Planar Engineering — Secrets of Strixhaven #158
 * {3}{G} · Sorcery
 *
 * Sacrifice two lands. Search your library for four basic land cards, put them onto the
 * battlefield tapped, then shuffle.
 *
 * Resolution order matters: you sacrifice first (so the cost is paid before you ramp), then
 * search for the basics. Composed from [Effects.Sacrifice] (the controller sacrifices their own
 * lands) + [Patterns.Library.searchLibrary] (an Explosive Vegetation-style fetch to the
 * battlefield tapped, here for four basics).
 */
val PlanarEngineering = card("Planar Engineering") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Sacrifice two lands. Search your library for four basic land cards, put them " +
        "onto the battlefield tapped, then shuffle."

    spell {
        effect = Effects.Composite(
            Effects.Sacrifice(GameObjectFilter.Land, count = 2, target = EffectTarget.PlayerRef(Player.You)),
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 4,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true,
                shuffleAfter = true,
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "158"
        artist = "Liiga Smilshkalne"
        flavorText = "\"All growth comes at a cost. It's up to each of us to determine if the " +
            "result is worth the sacrifice.\"\n—Oracle Jadzi, to Tam"
        imageUri = "https://cards.scryfall.io/normal/front/c/8/c83b96a3-ddfd-4d11-8a85-5bf62087cbb9.jpg?1775938080"
    }
}
