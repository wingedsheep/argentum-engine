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
 * Urza's Tower
 * Land — Urza's Tower
 * {T}: Add {C}. If you control an Urza's Mine and an Urza's Power-Plant, add {C}{C}{C} instead.
 */
val UrzasTower = card("Urza's Tower") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land — Urza's Tower"
    oracleText = "{T}: Add {C}. If you control an Urza's Mine and an Urza's Power-Plant, add {C}{C}{C} instead."

    activatedAbility {
        cost = Costs.Tap
        effect = ConditionalEffect(
            condition = Conditions.All(
                Conditions.YouControl(GameObjectFilter.Land.named("Urza's Mine")),
                Conditions.YouControl(GameObjectFilter.Land.named("Urza's Power-Plant"))
            ),
            effect = Effects.AddColorlessMana(3),
            elseEffect = Effects.AddColorlessMana(1)
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {C}. If you control an Urza's Mine and an Urza's Power-Plant, add {C}{C}{C} instead."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85a"
        artist = "Mark Poole"
        flavorText = "Urza always put Tocasia's lessons on resource-gathering to effective use."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8ed85655-fc59-4a57-bcf9-75e1899dff78.jpg?1562925143"
    }
}
