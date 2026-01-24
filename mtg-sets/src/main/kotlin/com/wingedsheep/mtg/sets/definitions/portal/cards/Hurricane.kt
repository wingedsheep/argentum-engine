package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreatureDamageFilter
import com.wingedsheep.sdk.scripting.DealXDamageToAllEffect

/**
 * Hurricane
 * {X}{G}
 * Sorcery
 * Hurricane deals X damage to each creature with flying and each player.
 */
val Hurricane = card("Hurricane") {
    manaCost = "{X}{G}"
    typeLine = "Sorcery"

    spell {
        effect = DealXDamageToAllEffect(
            creatureFilter = CreatureDamageFilter.WithKeyword(Keyword.FLYING),
            includePlayers = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "170"
        artist = "Cornelius Brudi"
        flavorText = "The wind knows no master but the sky."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08a77e61-45ae-4d16-9fbe-8d5e93f8f8fb.jpg"
    }
}
