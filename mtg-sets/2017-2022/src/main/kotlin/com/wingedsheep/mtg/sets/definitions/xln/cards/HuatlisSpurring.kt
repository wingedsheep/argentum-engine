package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

val HuatlisSpurring = card("Huatli's Spurring") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+0 until end of turn. If you control a Huatli planeswalker, " +
        "that creature gets +4/+0 until end of turn instead."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.If(
            condition = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Huatli")),
            then = Effects.ModifyStats(4, 0, creature),
            otherwise = Effects.ModifyStats(2, 0, creature),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "287"
        artist = "Zack Stella"
        flavorText = "\"Run like the wind, fierce one. The hunt has begun.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a19440e2-855f-4f60-a443-b99500394611.jpg?1783935680"
        ruling("2017-09-29", "Whether you control a Huatli planeswalker is checked only as Huatli’s Spurring resolves. " +
            "The creature’s power won’t change later in the turn if Huatli leaves or comes under your control.")
    }
}
