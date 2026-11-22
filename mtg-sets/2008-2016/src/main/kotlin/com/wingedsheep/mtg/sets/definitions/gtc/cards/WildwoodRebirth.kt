package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wildwood Rebirth
 * {1}{G}
 * Instant
 * Return target creature card from your graveyard to your hand.
 */
val WildwoodRebirth = card("Wildwood Rebirth") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Return target creature card from your graveyard to your hand."

    spell {
        val t = target(TargetFilter.CreatureInYourGraveyard)
        effect = Effects.ReturnToHand(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Dan Murayama Scott"
        flavorText = "In Ravnica, there are maladies far more difficult to treat than death."
        imageUri = "https://cards.scryfall.io/normal/front/7/1/713a93a1-4442-4d5b-ad7a-136b87b5f7ab.jpg"
    }
}
