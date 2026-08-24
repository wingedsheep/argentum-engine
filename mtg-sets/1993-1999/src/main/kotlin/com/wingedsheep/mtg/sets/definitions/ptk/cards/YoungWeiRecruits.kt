package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock

/**
 * Young Wei Recruits
 * {1}{B}
 * Creature — Human Soldier
 * 2/2
 * This creature can't block.
 */
val YoungWeiRecruits = card("Young Wei Recruits") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "This creature can't block."

    staticAbility {
        ability = CantBlock()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "94"
        artist = "Li Youliang"
        flavorText = "\"To send the common people to war untrained is to throw them away.\"\n—Confucius, *The Analects* (trans. Lau)"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/11431c4c-b0bb-4747-a34a-4a90238ec9c6.jpg"
    }
}
