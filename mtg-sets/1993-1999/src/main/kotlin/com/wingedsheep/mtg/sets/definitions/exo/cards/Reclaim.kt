package com.wingedsheep.mtg.sets.definitions.exo.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Reclaim
 * {G}
 * Instant
 * Put target card from your graveyard on top of your library.
 */
val Reclaim = card("Reclaim") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put target card from your graveyard on top of your library."

    spell {
        val t = target(TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.PutOnTopOfLibrary(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Andrew Robinson"
        flavorText = "The wise pay as much attention to what they throw away as to what they keep."
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b47c082-57f6-4f69-87e8-a07cad9ef042.jpg?1783946504"
    }
}
