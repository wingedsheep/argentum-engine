package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreatureDamageFilter
import com.wingedsheep.sdk.scripting.DealXDamageToAllEffect

/**
 * Earthquake
 * {X}{R}
 * Sorcery
 * Earthquake deals X damage to each creature without flying and each player.
 */
val Earthquake = card("Earthquake") {
    manaCost = "{X}{R}"
    typeLine = "Sorcery"

    spell {
        effect = DealXDamageToAllEffect(
            creatureFilter = CreatureDamageFilter.WithoutKeyword(Keyword.FLYING),
            includePlayers = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Dan Frazier"
        flavorText = "The ground itself becomes a weapon."
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4f5a6b7-c8d9-0e1f-2a3b-4c5d6e7f8a9b.jpg"
    }
}
