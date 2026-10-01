package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.forMirrodin
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Dragonwing Glider
 * {3}{R}{R}
 * Artifact — Equipment
 * For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)
 * Equipped creature gets +2/+2 and has flying and haste.
 * Equip {3}{R}{R}
 */
val DragonwingGlider = card("Dragonwing Glider") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)\n" +
        "Equipped creature gets +2/+2 and has flying and haste.\n" +
        "Equip {3}{R}{R}"

    forMirrodin()

    staticAbility {
        ability = ModifyStats(2, 2, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, Filters.EquippedCreature)
    }

    equipAbility("{3}{R}{R}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "Andreas Zafiratos"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/5456b036-231e-4a64-b060-0709f5254664.jpg?1783918031"
    }
}
