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
 * Urza's Mine
 * Land — Urza's Mine
 * {T}: Add {C}. If you control an Urza's Power-Plant and an Urza's Tower, add {C}{C} instead.
 */
val UrzasMine = card("Urza's Mine") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land — Urza's Mine"
    oracleText = "{T}: Add {C}. If you control an Urza's Power-Plant and an Urza's Tower, add {C}{C} instead."

    activatedAbility {
        cost = Costs.Tap
        effect = ConditionalEffect(
            condition = Conditions.All(
                Conditions.YouControl(GameObjectFilter.Land.named("Urza's Power-Plant")),
                Conditions.YouControl(GameObjectFilter.Land.named("Urza's Tower"))
            ),
            effect = Effects.AddColorlessMana(2),
            elseEffect = Effects.AddColorlessMana(1)
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {C}. If you control an Urza's Power-Plant and an Urza's Tower, add {C}{C} instead."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "83c"
        artist = "Anson Maddocks"
        flavorText = "Mines became common as cities during the days of the artificers."
        imageUri = "https://cards.scryfall.io/normal/front/d/a/da68a5c0-84fe-4a8f-93b2-790eca3cc95c.jpg?1562941120"
    }
}
