package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hedron Crawler
 * {2}
 * Artifact Creature — Construct
 * 0/1
 * {T}: Add {C}. ({C} represents colorless mana.)
 */
val HedronCrawler = card("Hedron Crawler") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 0
    toughness = 1
    oracleText = "{T}: Add {C}. ({C} represents colorless mana.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Daniel Ljunggren"
        flavorText = "\"It's endearing, in a mystical, pointy sort of way.\"\n—General Tazri, allied commander"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9f827f92-1df6-4fd0-aa61-ec2e53476f9c.jpg"
    }
}
