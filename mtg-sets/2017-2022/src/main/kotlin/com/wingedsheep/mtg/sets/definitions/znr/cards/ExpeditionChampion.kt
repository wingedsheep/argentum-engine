package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Expedition Champion
 * {2}{R}
 * Creature — Human Warrior
 * 2/3
 *
 * This creature gets +2/+0 as long as you control another Warrior.
 *
 * Same "another <party class>" shape as Expedition Skulker / Expedition Healer (excludeSelf).
 */
val ExpeditionChampion = card("Expedition Champion") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 3
    oracleText = "This creature gets +2/+0 as long as you control another Warrior."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 2, toughnessBonus = 0, filter = GroupFilter.source()),
            condition = Conditions.YouControl(
                GameObjectFilter.Permanent.withSubtype(Subtype.WARRIOR),
                excludeSelf = true
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Bram Sels"
        flavorText = "Warriors are combat specialists who bring their deadly skills to bear against the many " +
            "foes that adventurers face in the course of their journeys."
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30084dc1-f501-4b7c-972d-1a3b9137083a.jpg?1783929361"
        ruling(
            "2020-09-25",
            "Expedition Champion gets just +2/+0, no matter how many other Warriors you control beyond the first."
        )
    }
}
