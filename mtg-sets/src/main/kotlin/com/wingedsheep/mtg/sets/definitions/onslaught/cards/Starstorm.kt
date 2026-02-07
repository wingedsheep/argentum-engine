package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageToGroupEffect
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Starstorm
 * {X}{R}{R}
 * Instant
 * Starstorm deals X damage to each creature.
 * Cycling {3}
 */
val Starstorm = card("Starstorm") {
    manaCost = "{X}{R}{R}"
    typeLine = "Instant"

    spell {
        effect = DealDamageToGroupEffect(DynamicAmount.XValue)
    }

    keywordAbility(KeywordAbility.cycling("{3}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "238"
        artist = "David Martin"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/large/front/b/5/b54d72ba-05ce-4299-a7c3-a9e9f126fffb.jpg?1562937719"
    }
}
