package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Chariot of Victory
 * {3}
 * Artifact — Equipment
 *
 * Equipped creature has first strike, trample, and haste.
 * Equip {1}
 *
 * One Layer 6 grant per keyword over the equipped creature (GrantKeyword's default scope).
 */
val ChariotOfVictory = card("Chariot of Victory") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has first strike, trample, and haste.\nEquip {1}"

    staticAbility {
        ability = GrantKeyword(Keyword.FIRST_STRIKE)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE)
    }
    staticAbility {
        ability = GrantKeyword(Keyword.HASTE)
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "John Stanko"
        flavorText = "The greatest heroes are the first to stand, the first to take up arms, and the first to meet the enemy."
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2dd03d2-05ef-4929-b973-3dbe49fc7592.jpg?1783939402"
    }
}
