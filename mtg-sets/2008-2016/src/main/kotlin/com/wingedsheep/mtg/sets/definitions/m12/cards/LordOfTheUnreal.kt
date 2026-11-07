package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Lord of the Unreal
 * {U}{U}
 * Creature — Human Wizard
 * 2/2
 *
 * Illusion creatures you control get +1/+1 and have hexproof.
 *
 * Both statics read the same "Illusion creatures you control" group; the Lord is a Human Wizard,
 * so it never buffs itself.
 */
val LordOfTheUnreal = card("Lord of the Unreal") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "Illusion creatures you control get +1/+1 and have hexproof. " +
        "(They can't be the targets of spells or abilities your opponents control.)"

    staticAbility {
        ability = ModifyStats(1, 1, GroupFilter(GameObjectFilter.Creature.withSubtype("Illusion").youControl()))
    }

    staticAbility {
        ability = GrantKeyword(Keyword.HEXPROOF, GroupFilter(GameObjectFilter.Creature.withSubtype("Illusion").youControl()))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "62"
        artist = "Jason Chan"
        flavorText = "\"The dream does not end until I say so.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b09140f6-fa75-4bee-9ca0-3a71cd2b5a7b.jpg?1783941091"
    }
}
