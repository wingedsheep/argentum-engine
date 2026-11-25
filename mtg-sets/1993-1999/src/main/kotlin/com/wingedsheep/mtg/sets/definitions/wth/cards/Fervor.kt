package com.wingedsheep.mtg.sets.definitions.wth.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Fervor
 * {2}{R}
 * Enchantment
 * Creatures you control have haste.
 */
val Fervor = card("Fervor") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Creatures you control have haste. (They can attack and {T} as soon as they come under your control.)"

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.HASTE,
            filter = GroupFilter(GameObjectFilter.Creature.youControl()),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "99"
        artist = "Franz Vohwinkel"
        flavorText = "\"If your blood doesn't run hot, I will make it run in the sands!\"\n—Maraxus of Keld"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b4df70ea-2b6b-4e25-a564-655989ef16fa.jpg?1783946728"
    }
}
