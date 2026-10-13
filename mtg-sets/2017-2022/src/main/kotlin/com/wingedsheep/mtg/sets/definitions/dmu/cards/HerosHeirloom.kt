package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Hero's Heirloom
 * {2}
 * Artifact — Equipment
 *
 * Equipped creature gets +2/+1.
 * As long as equipped creature is legendary, it has trample and haste.
 * Equip {2}
 */
val HerosHeirloom = card("Hero's Heirloom") {
    manaCost = "{2}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+1.\n" +
        "As long as equipped creature is legendary, it has trample and haste.\n" +
        "Equip {2}"

    // Equipped creature gets +2/+1.
    staticAbility {
        ability = ModifyStats(+2, +1, Filters.EquippedCreature)
    }

    // As long as equipped creature is legendary, it has trample and haste.
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.TRAMPLE),
            condition = Conditions.EnchantedCreatureIsLegendary()
        )
    }
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HASTE),
            condition = Conditions.EnchantedCreatureIsLegendary()
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Ovidio Cartagena"
        flavorText = "\"A blade is just a tool. Its power comes from the hand that wields it.\"\n—Danitha"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b7c0d2d-7107-4599-8d49-c7c57d21e467.jpg?1783921268"
    }
}
