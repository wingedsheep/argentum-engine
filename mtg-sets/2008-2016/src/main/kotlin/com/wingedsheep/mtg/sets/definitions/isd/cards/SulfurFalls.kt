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
 * Sulfur Falls
 * Land
 * This land enters tapped unless you control an Island or a Mountain.
 * {T}: Add {U} or {R}.
 */
val SulfurFalls = card("Sulfur Falls") {
    typeLine = "Land"
    colorIdentity = "UR"
    oracleText = "This land enters tapped unless you control an Island or a Mountain.\n{T}: Add {U} or {R}."

    replacementEffect(EntersTapped(
        unlessCondition = Conditions.Any(
            Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Island")),
            Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Land.withSubtype("Mountain"))
        )
    ))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        flavorText = "Skirsdag cultists meet behind a waterfall's curtain and plot with demonic forces."
        rarity = Rarity.RARE
        collectorNumber = "248"
        artist = "Cliff Childs"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/4968b65d-50e5-4d7e-b78b-cdada1cbf7a7.jpg"
    }
}
