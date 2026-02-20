package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Renewing Dawn
 * {1}{W}
 * Sorcery
 * You gain 2 life for each Mountain target opponent controls.
 */
val RenewingDawn = card("Renewing Dawn") {
    manaCost = "{1}{W}"
    typeLine = "Sorcery"

    spell {
        target = TargetOpponent()
        effect = GainLifeEffect(
            DynamicAmounts.landsOfTypeTargetOpponentControls(landType = "Mountain", multiplier = 2)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "John Avon"
        flavorText = "Dawn brings a new day, and a new day brings hope."
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e56300cb-6b44-47fe-9508-c33ad5670b4b.jpg"
    }
}
