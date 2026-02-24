package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost

/**
 * Bloodline Shaman
 * {1}{G}
 * Creature — Elf Wizard Shaman
 * 1/1
 * {T}: Choose a creature type. Reveal the top card of your library. If that card
 * is a creature card of the chosen type, put it into your hand. Otherwise, put it
 * into your graveyard.
 */
val BloodlineShaman = card("Bloodline Shaman") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Elf Wizard Shaman"
    power = 1
    toughness = 1
    oracleText = "{T}: Choose a creature type. Reveal the top card of your library. If that card is a creature card of the chosen type, put it into your hand. Otherwise, put it into your graveyard."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = EffectPatterns.chooseCreatureTypeRevealTop()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "249"
        artist = "Rebecca Guay"
        flavorText = "\"Every creature of the forest has a name, and she knows them all.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5fdfc473-8477-4c04-a4e7-ecac1b0a5716.jpg?1562917608"
    }
}
