package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Evra, Halcyon Witness
 * {4}{W}{W}
 * Legendary Creature — Avatar
 * 4/4
 * Lifelink
 * {4}: Exchange your life total with Evra, Halcyon Witness's power.
 */
val EvraHalcyonWitness = card("Evra, Halcyon Witness") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Avatar"
    power = 4
    toughness = 4
    oracleText = "Lifelink\n{4}: Exchange your life total with Evra, Halcyon Witness's power."

    keywords(Keyword.LIFELINK)

    activatedAbility {
        cost = Costs.Mana("{4}")
        effect = Effects.ExchangeLifeAndStat()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "16"
        artist = "Johannes Voss"
        flavorText = "Light from the Null Moon took form—a mirage made real, alone in grandeur, isolated in a world it did not comprehend."
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02f57a57-8ce8-4d01-9b91-99ec0623d1e9.jpg?1562730663"
    }
}
