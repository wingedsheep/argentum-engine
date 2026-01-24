package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.RevealHandDrawPerMatchEffect
import com.wingedsheep.sdk.targeting.TargetOpponent

/**
 * Withering Gaze
 * {2}{U}
 * Sorcery
 * Target opponent reveals their hand. You draw a card for each Forest and green card in it.
 */
val WitheringGaze = card("Withering Gaze") {
    manaCost = "{2}{U}"
    typeLine = "Sorcery"

    spell {
        target = TargetOpponent()
        effect = RevealHandDrawPerMatchEffect(
            landType = "Forest",
            color = Color.GREEN
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Anthony S. Waters"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/194d94d2-3e1f-4e1b-8e49-4d2b27e10fe0.jpg"
    }
}
