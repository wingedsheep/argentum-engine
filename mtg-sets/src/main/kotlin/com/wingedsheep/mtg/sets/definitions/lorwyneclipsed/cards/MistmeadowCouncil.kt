package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SpellCostReduction

val MistmeadowCouncil = card("Mistmeadow Council") {
    manaCost = "{4}{G}"
    typeLine = "Creature — Kithkin Advisor"
    power = 4
    toughness = 3
    oracleText = "This spell costs {1} less to cast if you control a Kithkin.\n" +
        "When this creature enters, draw a card."

    staticAbility {
        ability = SpellCostReduction(
            CostReductionSource.FixedIfControlFilter(
                amount = 1,
                filter = GameObjectFilter.Any.withSubtype("Kithkin")
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "183"
        artist = "Jim Pavelec"
        flavorText = "\"You've been hiding something. Everyone can tell.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4a7c9bc-81c8-4c31-96a9-eb6ba7715e7f.jpg?1767872109"
    }
}
