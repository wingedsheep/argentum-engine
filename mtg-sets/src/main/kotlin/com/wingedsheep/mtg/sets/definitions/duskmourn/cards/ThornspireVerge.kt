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
 * Thornspire Verge
 * Land
 *
 * {T}: Add {R}.
 * {T}: Add {G}. Activate only if you control a Mountain or a Forest.
 */
val ThornspireVerge = card("Thornspire Verge") {
    typeLine = "Land"
    oracleText = "{T}: Add {R}.\n{T}: Add {G}. Activate only if you control a Mountain or a Forest."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddManaEffect(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.Any(
                    Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Mountain")),
                    Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Forest"))
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "270"
        artist = "Kasia 'Kafis' Zielińska"
        flavorText = "Here, the Boilerbilges twitch like a trapped animal, peaks straining against the grasping roots and vines."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e1cdc03-6faa-4138-9a52-caafbe34fb59.jpg?1773857342"
    }
}
