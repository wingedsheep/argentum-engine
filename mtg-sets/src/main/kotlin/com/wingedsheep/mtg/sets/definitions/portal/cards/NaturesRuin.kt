package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DestroyAllEffect
import com.wingedsheep.sdk.targeting.PermanentTargetFilter

/**
 * Nature's Ruin
 * {2}{B}
 * Sorcery
 * Destroy all green creatures.
 */
val NaturesRuin = card("Nature's Ruin") {
    manaCost = "{2}{B}"
    typeLine = "Sorcery"

    spell {
        effect = DestroyAllEffect(
            PermanentTargetFilter.And(
                listOf(PermanentTargetFilter.Creature, PermanentTargetFilter.WithColor(Color.GREEN))
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "Jeff Miracola"
        flavorText = "The forest withers and dies as darkness spreads."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/5950f52a-493e-432e-9175-0272c0edb232.jpg"
    }
}
