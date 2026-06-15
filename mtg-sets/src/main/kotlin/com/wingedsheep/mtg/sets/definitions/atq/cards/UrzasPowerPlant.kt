package com.wingedsheep.mtg.sets.definitions.atq.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Urza's Power Plant
 * Land — Urza's Power-Plant
 * {T}: Add {C}. If you control an Urza's Mine and an Urza's Tower, add {C}{C} instead.
 */
val UrzasPowerPlant = card("Urza's Power Plant") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land — Urza's Power-Plant"
    oracleText = "{T}: Add {C}. If you control an Urza's Mine and an Urza's Tower, add {C}{C} instead."

    activatedAbility {
        cost = Costs.Tap
        effect = ConditionalEffect(
            condition = Conditions.All(
                Conditions.YouControl(GameObjectFilter.Land.named("Urza's Mine")),
                Conditions.YouControl(GameObjectFilter.Land.named("Urza's Tower"))
            ),
            effect = Effects.AddColorlessMana(2),
            elseEffect = Effects.AddColorlessMana(1)
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {C}. If you control an Urza's Mine and an Urza's Tower, add {C}{C} instead."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84a"
        artist = "Mark Tedin"
        flavorText = "Artifact construction required immense resources."
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94896e0b-859c-47e4-bf27-35ed37b841e0.jpg?1562926427"
    }
}
