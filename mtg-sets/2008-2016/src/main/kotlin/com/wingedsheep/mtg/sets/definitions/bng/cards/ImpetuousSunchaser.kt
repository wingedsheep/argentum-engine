package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack

/**
 * Impetuous Sunchaser
 * {1}{R}
 * Creature — Human Soldier
 * 1/1
 *
 * Flying, haste
 * This creature attacks each combat if able.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val ImpetuousSunchaser = card("Impetuous Sunchaser") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "Flying, haste\n" +
        "This creature attacks each combat if able."

    keywords(Keyword.FLYING, Keyword.HASTE)

    staticAbility {
        ability = MustAttack()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "99"
        artist = "Cynthia Sheppard"
        flavorText = "\"I will soar higher than the walls of Akros! Higher than the clouds! Higher than Heliod himself!\""
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6d4c13b-95ef-49df-b349-26e0fec109ad.jpg?1783939545"
    }
}
