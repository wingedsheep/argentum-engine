package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Commune with Evil
 * {2}{B}
 * Sorcery
 *
 * Look at the top four cards of your library. Put one of them into your hand and the rest
 * into your graveyard. You gain 3 life.
 */
val CommuneWithEvil = card("Commune with Evil") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Look at the top four cards of your library. Put one of them into your hand and the rest into your graveyard. You gain 3 life."

    spell {
        effect = Effects.Composite(
            Patterns.Library.lookAtTopAndKeep(count = 4, keepCount = 1),
            Effects.GainLife(3),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Ovidio Cartagena"
        flavorText = "Elsa swelled with euphoria and power as she stepped over her friends' corpses. She should have bargained with a demon a lot sooner."
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9ab94ebe-b51b-4c6f-af43-0ed80e2808a3.jpg?1726286174"
    }
}
