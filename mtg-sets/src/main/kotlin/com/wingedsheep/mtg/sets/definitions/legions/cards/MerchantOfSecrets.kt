package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Merchant of Secrets
 * {2}{U}
 * Creature — Human Wizard
 * 1/1
 * When Merchant of Secrets enters the battlefield, draw a card.
 */
val MerchantOfSecrets = card("Merchant of Secrets") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "Greg Hildebrandt"
        flavorText = "To scrape out a living in Aphetto, wizards are reduced to selling rumors, lies, forgeries, or—if they get desperate enough—the truth."
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1109bdd-a5ce-4e63-adee-54e43a4c4a1e.jpg?1562937017"
    }
}
