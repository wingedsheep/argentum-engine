package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageToGroupEffect
import com.wingedsheep.sdk.scripting.DealDamageToPlayersEffect

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
        effect = DealDamageToGroupEffect(6) then DealDamageToPlayersEffect(6)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "Dan Frazier"
        flavorText = "A storm of fire consumes all in its path."
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92334ebe-3d7a-46de-8b91-931e5d56a5a5.jpg"
    }
}
