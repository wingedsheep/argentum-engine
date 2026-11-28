package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Woodland Cemetery
 * Land
 * This land enters tapped unless you control a Swamp or a Forest.
 * {T}: Add {B} or {G}.
 */
val WoodlandCemetery = card("Woodland Cemetery") {
    typeLine = "Land"
    colorIdentity = "BG"
    oracleText = "This land enters tapped unless you control a Swamp or a Forest.\n{T}: Add {B} or {G}."

    replacementEffect(EntersTapped(
        unlessCondition = Conditions.Any(
            Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Swamp")),
            Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Forest"))
        )
    ))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        flavorText = "Farmers cut the trees and buried their dead. The dead rose up and killed the farmers. The trees grew back and strangled the dead."
        rarity = Rarity.RARE
        collectorNumber = "249"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67139101-ec5e-434b-be3a-21338cc33840.jpg"
    }
}
