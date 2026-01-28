package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreatureDamageFilter
import com.wingedsheep.sdk.scripting.DealDamageToGroupEffect
import com.wingedsheep.sdk.scripting.DealDamageToPlayersEffect
import com.wingedsheep.sdk.scripting.DynamicAmount

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
        effect = DealDamageToGroupEffect(DynamicAmount.XValue, CreatureDamageFilter.WithoutKeyword(Keyword.FLYING)) then
            DealDamageToPlayersEffect(DynamicAmount.XValue)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Dan Frazier"
        flavorText = "The ground itself becomes a weapon."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/272f65a3-3c0c-417d-b5b6-276a643d643e.jpg"
    }
}
