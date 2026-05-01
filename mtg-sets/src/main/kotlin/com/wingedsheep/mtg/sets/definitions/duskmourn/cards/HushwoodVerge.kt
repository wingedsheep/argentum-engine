package com.wingedsheep.mtg.sets.definitions.duskmourn.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Hushwood Verge
 * Land
 *
 * {T}: Add {G}.
 * {T}: Add {W}. Activate only if you control a Forest or a Plains.
 */
val HushwoodVerge = card("Hushwood Verge") {
    typeLine = "Land"
    oracleText = "{T}: Add {G}.\n{T}: Add {W}. Activate only if you control a Forest or a Plains."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.Any(
                    Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Forest")),
                    Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Plains"))
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "261"
        artist = "Kasia 'Kafis' Zielińska"
        flavorText = "Here, the Hauntwoods churn like a viper's nest, as monstrous creatures prowl hungrily toward the ghostly light."
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec288d76-c1f5-471b-8a53-504f88469c1b.jpg?1773857339"
    }
}
