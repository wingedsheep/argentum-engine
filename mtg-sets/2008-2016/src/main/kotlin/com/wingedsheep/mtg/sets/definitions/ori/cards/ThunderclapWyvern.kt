package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Thunderclap Wyvern
 * {2}{W}{U}
 * Creature — Drake
 * 2/3
 * Flash
 * Flying
 * Other creatures you control with flying get +1/+1.
 */
val ThunderclapWyvern = card("Thunderclap Wyvern") {
    manaCost = "{2}{W}{U}"
    colorIdentity = "UW"
    typeLine = "Creature — Drake"
    power = 2
    toughness = 3
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\nFlying\nOther creatures you control with flying get +1/+1."

    keywords(Keyword.FLASH, Keyword.FLYING)

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withKeyword(Keyword.FLYING).youControl(),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Jason Felix"
        flavorText = "Thunder doesn't always mean rain. Sometimes it means ruin."
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd20971a-11aa-452b-8507-0f48229062a0.jpg?1783938312"
    }
}
