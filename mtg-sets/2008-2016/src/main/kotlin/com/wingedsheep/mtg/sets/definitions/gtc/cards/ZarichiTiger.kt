package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Zarichi Tiger
 * {3}{W}
 * Creature — Cat
 * 2/3
 * {1}{W}, {T}: You gain 2 life.
 */
val ZarichiTiger = card("Zarichi Tiger") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat"
    oracleText = "{1}{W}, {T}: You gain 2 life."
    power = 2
    toughness = 3

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{W}"), Costs.Tap)
        effect = Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "28"
        artist = "Nic Klein"
        flavorText = "\"The taming and consecration of these creatures is evidence of our faith.\"\n—Resimir, Zarichi Temple priest"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7bf5efe4-d9a0-4704-b5ba-3213c946df37.jpg"
    }
}
