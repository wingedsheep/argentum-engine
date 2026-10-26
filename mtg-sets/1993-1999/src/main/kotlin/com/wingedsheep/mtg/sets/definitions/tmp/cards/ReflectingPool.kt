package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.LandControllerScope

/**
 * Reflecting Pool
 * Land
 * {T}: Add one mana of any type that a land you control could produce.
 *
 * "Any type" is the five colors plus colorless, authored as Naga Vitalist is: the colors through
 * `LandsCouldProduce(YOU)`, and {C} as a second mana ability gated on a land you control that could
 * produce colorless. The engine reads both halves as reflection and excludes the Pool itself, so a
 * Pool never reflects its own abilities and two Pools with no other lands produce nothing.
 */
val ReflectingPool = card("Reflecting Pool") {
    typeLine = "Land"
    oracleText = "{T}: Add one mana of any type that a land you control could produce."

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddManaOfColorLandsCouldProduce(LandControllerScope.YOU)
        description = "{T}: Add one mana of any color that a land you control could produce."
    }

    activatedAbility {
        cost = Costs.Tap
        manaAbility = true
        effect = Effects.AddColorlessMana(1)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouControl(GameObjectFilter.Land.couldProduceColorlessMana())
            ),
        )
        description = "{T}: Add {C}. Activate only if a land you control could produce colorless mana."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "322"
        artist = "Adam Rex"
        flavorText = "\"The reflection of land interests me far more than its reality.\"\n—Volrath"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b633ac2-bf98-42bd-8c26-8737a7d8edc7.jpg?1783946597"
    }
}
