package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DrawCardsEffect
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
            count = DynamicAmounts.tappedCreaturesTargetOpponentControls(),
            target = EffectTarget.Controller
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "72"
        artist = "Brian Snoddy"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29019e28-4ef8-4732-9972-0a47305fe303.jpg"
    }
}
