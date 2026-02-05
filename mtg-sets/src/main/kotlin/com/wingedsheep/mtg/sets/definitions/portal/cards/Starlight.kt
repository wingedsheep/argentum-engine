package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GainLifeEffect
import com.wingedsheep.sdk.targeting.TargetOpponent

/**
 * Starlight
 * {1}{W}
 * Sorcery
 * You gain 3 life for each black creature target opponent controls.
 */
val Starlight = card("Starlight") {
    manaCost = "{1}{W}"
    typeLine = "Sorcery"

    spell {
        target = TargetOpponent()
        effect = GainLifeEffect(
            DynamicAmounts.creaturesOfColorTargetOpponentControls(color = Color.BLACK, multiplier = 3)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "John Avon"
        flavorText = "Stars are like coins dropped into the night by an angel."
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6992524-6921-473b-8301-cb63fe502600.jpg"
    }
}
