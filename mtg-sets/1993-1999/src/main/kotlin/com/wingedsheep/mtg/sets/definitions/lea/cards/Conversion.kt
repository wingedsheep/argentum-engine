package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.SetLandTypesForGroup
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Conversion — every Mountain (any controller, basic or not) becomes a Plains: it loses its other
 * land types and the abilities they granted and taps for {W} instead (CR 305.7). The type change
 * and ability loss are one multi-layer ability, so the affected set is locked in layer 4 (CR 613.6)
 * even though the lands stop being Mountains once it applies.
 */
val Conversion = card("Conversion") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, sacrifice this enchantment unless you pay {W}{W}.\n" +
        "All Mountains are Plains."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.PayOrSuffer(cost = Costs.pay.Mana("{W}{W}"), suffer = SacrificeSelfEffect)
    }

    staticAbility {
        ability = SetLandTypesForGroup(
            filter = GroupFilter(GameObjectFilter.Land.withSubtype("Mountain")),
            landTypes = setOf("Plains"),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Jesper Myrfors"
        imageUri = "https://cards.scryfall.io/normal/front/1/3/13186bc9-8d9c-433b-ba15-121ef94dd68a.jpg?1783948715"
        ruling("2006-10-15", "Will not add or remove the supertype snow to or from a land.")
        ruling("2004-10-04", "The Conversion effect is a continuous effect. There is no chance to tap a just-played mountain for red mana before it becomes a plains.")
    }
}
