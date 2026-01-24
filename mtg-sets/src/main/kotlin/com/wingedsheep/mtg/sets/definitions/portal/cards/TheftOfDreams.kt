package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DrawCardsEffect
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.targeting.TargetOpponent

/**
 * Theft of Dreams
 * {2}{U}
 * Sorcery
 * Draw a card for each tapped creature target opponent controls.
 */
val TheftOfDreams = card("Theft of Dreams") {
    manaCost = "{2}{U}"
    typeLine = "Sorcery"

    spell {
        target = TargetOpponent()
        effect = DrawCardsEffect(
            count = DynamicAmount.TappedCreaturesTargetOpponentControls,
            target = EffectTarget.Controller
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "72"
        artist = "Brian Snoddy"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c0d8c1f-a8d7-4d75-b276-d15b2c49b4d0.jpg"
    }
}
