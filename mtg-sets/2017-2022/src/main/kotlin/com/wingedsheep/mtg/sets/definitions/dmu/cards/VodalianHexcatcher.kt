package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val VodalianHexcatcher = card("Vodalian Hexcatcher") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Merfolk Wizard"
    power = 1
    toughness = 1
    oracleText = "Flash\nOther Merfolk you control get +1/+1.\n" +
        "Sacrifice a Merfolk: Counter target noncreature spell unless its controller pays {1}."

    keywords(Keyword.FLASH)

    staticAbility {
        ability = ModifyStats(
            1, 1,
            GroupFilter(GameObjectFilter.Permanent.withSubtype("Merfolk").youControl(), excludeSelf = true),
        )
    }

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Permanent.withSubtype("Merfolk"))
        target(TargetFilter.NoncreatureSpellOnStack)
        effect = Effects.CounterUnlessPays("{1}")
        description = "Sacrifice a Merfolk: Counter target noncreature spell unless its controller pays {1}."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Dmitry Burmak"
        flavorText = "\"You're just one more pollutant to be removed from the sea.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4ec464dc-b1dd-4e45-b093-c3ad65a74050.jpg?1783921340"
    }
}
