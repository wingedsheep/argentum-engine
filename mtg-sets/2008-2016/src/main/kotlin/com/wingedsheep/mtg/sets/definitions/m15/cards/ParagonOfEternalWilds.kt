package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Paragon of Eternal Wilds
 * {3}{G}
 * Creature — Human Druid
 * 2/2
 * Other green creatures you control get +1/+1.
 * {G}, {T}: Another target green creature you control gains trample until end of turn. (It can deal excess combat damage to the player or planeswalker it's attacking.)
 */
val ParagonOfEternalWilds = card("Paragon of Eternal Wilds") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Druid"
    power = 2
    toughness = 2
    oracleText = "Other green creatures you control get +1/+1.\n" +
        "{G}, {T}: Another target green creature you control gains trample until end of turn. (It can deal excess combat damage to the player or planeswalker it's attacking.)"

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(
                GameObjectFilter.Creature.withColor(Color.GREEN).youControl(),
                excludeSelf = true
            )
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}"), Costs.Tap)
        val creature = target(TargetFilter.Creature.withColor(Color.GREEN).youControl().other())
        effect = Effects.GrantKeyword(Keyword.TRAMPLE, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "190"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97c3fcca-480f-4acf-b74f-0f37b46fa799.jpg"
    }
}
