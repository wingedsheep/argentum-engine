package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val ValiantVeteran = card("Valiant Veteran") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Kor Soldier"
    power = 2
    toughness = 2
    oracleText = "Other Soldiers you control get +1/+1.\n{3}{W}{W}, Exile this card from your graveyard: Put a +1/+1 counter on each Soldier you control."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Permanent.withSubtype("Soldier").youControl(), excludeSelf = true),
        )
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{W}{W}"), Costs.ExileSelf)
        activateFromZone = Zone.GRAVEYARD
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.withSubtype("Soldier").youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "38"
        artist = "Néstor Ossandón Leal"
        flavorText = "\"Let them come. We will show them that Argivian steel is stronger than any Phyrexian metal.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18b1a4ee-437f-4a08-b176-8342d526143f.jpg?1783921357"
        ruling("2022-09-09", "Exiling Valiant Veteran from your graveyard is part of the cost to activate its last ability. This means that once it has been activated it is no longer in the graveyard, and players can't respond to it by removing it from your graveyard to prevent you from activating it.")
    }
}
