package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.RollAdditionalDice

/**
 * Pixie Guide {1}{U}
 * Creature — Faerie
 * 1/3
 * Flying
 * Grant an Advantage — If you would roll one or more dice, instead roll that many dice plus one and
 * ignore the lowest roll.
 */
val PixieGuide = card("Pixie Guide") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie"
    oracleText = "Flying\nGrant an Advantage — If you would roll one or more dice, instead roll that many dice plus one and ignore the lowest roll."
    power = 1
    toughness = 3

    keywords(Keyword.FLYING)

    staticAbility {
        ability = RollAdditionalDice(extraDice = 1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Fariba Khamseh"
        flavorText = "Pixies are well known as pranksters, but some delight in more helpful magic."
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c65631b9-ca62-4851-9eca-9c760fb1a177.jpg?1783926512"
    }
}
