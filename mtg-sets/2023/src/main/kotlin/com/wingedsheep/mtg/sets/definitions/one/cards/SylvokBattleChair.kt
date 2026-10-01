package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.forMirrodin
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Sylvok Battle-Chair
 * {4}{G}{G}
 * Artifact — Equipment
 * For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)
 * Equipped creature gets +4/+4 and has trample.
 * Equip {5}{G}{G}
 */
val SylvokBattleChair = card("Sylvok Battle-Chair") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Artifact — Equipment"
    oracleText = "For Mirrodin! (When this Equipment enters, create a 2/2 red Rebel creature token, then attach this to it.)\n" +
        "Equipped creature gets +4/+4 and has trample.\n" +
        "Equip {5}{G}{G}"

    forMirrodin()

    staticAbility {
        ability = ModifyStats(4, 4, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, Filters.EquippedCreature)
    }

    equipAbility("{5}{G}{G}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "184"
        artist = "Alexander Mokhov"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71c1a1d0-5616-42f8-a59c-42c1ccd48d26.jpg?1783918011"
    }
}
