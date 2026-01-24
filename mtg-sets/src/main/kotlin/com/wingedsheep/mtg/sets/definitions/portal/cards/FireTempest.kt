package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageToAllEffect

/**
 * Fire Tempest
 * {5}{R}{R}
 * Sorcery
 * Fire Tempest deals 6 damage to each creature and each player.
 */
val FireTempest = card("Fire Tempest") {
    manaCost = "{5}{R}{R}"
    typeLine = "Sorcery"

    spell {
        effect = DealDamageToAllEffect(6)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "Dan Frazier"
        flavorText = "A storm of fire consumes all in its path."
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28a9b0c1-d2e3-f4a5-b6c7-d8e9f0a1b2c3.jpg"
    }
}
