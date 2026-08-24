package com.wingedsheep.mtg.sets.definitions.tla.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Merchant of Many Hats
 * {1}{B}
 * Creature — Human Peasant Ally
 * 2/2
 * {2}{B}: Return this card from your graveyard to your hand.
 */
val MerchantOfManyHats = card("Merchant of Many Hats") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Peasant Ally"
    power = 2
    toughness = 2
    oracleText = "{2}{B}: Return this card from your graveyard to your hand."

    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        effect = Effects.ReturnToHandFromGraveyard(EffectTarget.Self)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "110"
        artist = "Boell Oyino"
        flavorText = "Also known as Dock the ferryman, Xu the fishmonger, and Bushi " +
            "the river cleaner, depending on what job needed to be done in Jang Hui village."
        imageUri = "https://cards.scryfall.io/normal/front/7/5/752ffa24-93b6-4b33-bf10-7222357ac472.jpg?1764120760"
    }
}
