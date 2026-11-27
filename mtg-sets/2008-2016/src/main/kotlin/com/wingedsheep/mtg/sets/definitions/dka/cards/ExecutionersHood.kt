package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Executioner's Hood
 * {2}
 * Artifact — Equipment
 * Equipped creature has intimidate.
 * Equip {2}
 */
val ExecutionersHood = card("Executioner's Hood") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has intimidate. (This creature can't be blocked except by artifact creatures and/or creatures that share a color with it.)\nEquip {2} ({2}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = GrantKeyword(Keyword.INTIMIDATE)
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Anthony Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/7447d115-9b29-4086-8435-40c7c957f242.jpg"
    }
}
