package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.values.LandControllerScope

/**
 * Naga Vitalist
 * {1}{G}
 * Creature — Snake Druid
 * {T}: Add one mana of any type that a land you control could produce.
 * 1/2
 *
 * "Any type" is any color *or* colorless (CR 106.1b), read per CR 106.7. The one ability is authored as its two
 * halves, which together offer exactly the same tap-for-one-mana choice: the colors come from
 * `ManaColorSet.LandsCouldProduce(YOU)`, and `{C}` is a second mana ability that can be activated
 * only while a land you control could produce colorless mana (a Wastes, an Eldrazi Temple). Both
 * read the lands' mana abilities, not their costs or tapped state, so a tapped land still counts.
 */
val NagaVitalist = card("Naga Vitalist") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Snake Druid"
    power = 1
    toughness = 2
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
        rarity = Rarity.COMMON
        collectorNumber = "176"
        artist = "James Ryman"
        flavorText = "The lands of the God-Pharaoh are suffused with his breath."
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ce9a8551-8788-47ea-b774-477f0f3ff22a.jpg?1783936472"
    }
}
