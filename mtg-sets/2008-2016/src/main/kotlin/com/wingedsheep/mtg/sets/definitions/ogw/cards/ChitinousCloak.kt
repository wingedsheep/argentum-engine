package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Chitinous Cloak
 * {3}
 * Artifact — Equipment
 * Equipped creature gets +2/+2 and has menace. (It can't be blocked except by two or more creatures.)
 * Equip {3}
 */
val ChitinousCloak = card("Chitinous Cloak") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+2 and has menace. " +
        "(It can't be blocked except by two or more creatures.)\n" +
        "Equip {3}"

    staticAbility {
        ability = ModifyStats(2, 2)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE)
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "163"
        artist = "Chris Rahn"
        flavorText = "\"If we're going to survive, we have to use every tool at our disposal. Especially the disgusting ones.\"\n—Ryza, Oran-Rief scout"
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15e54603-af67-497f-afe0-88cfa4d00095.jpg"
    }
}
