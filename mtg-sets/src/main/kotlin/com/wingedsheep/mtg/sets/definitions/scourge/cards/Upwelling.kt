package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.PreventManaPoolEmptying

/**
 * Upwelling
 * {3}{G}
 * Enchantment
 * Players don't lose unspent mana as steps and phases end.
 */
val Upwelling = card("Upwelling") {
    manaCost = "{3}{G}"
    typeLine = "Enchantment"
    oracleText = "Players don't lose unspent mana as steps and phases end."

    staticAbility {
        ability = PreventManaPoolEmptying
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "131"
        artist = "John Avon"
        flavorText = "Once again, Kamahl felt the full force of the Mirari's pull, but he had learned much since the last time."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21ab4600-1f71-48fa-a291-f5c5628c7395.jpg?1562526538"
    }
}
