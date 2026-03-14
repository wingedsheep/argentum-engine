package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Cabal Stronghold
 * Land
 * {T}: Add {C}.
 * {3}, {T}: Add {B} for each basic Swamp you control.
 */
val CabalStronghold = card("Cabal Stronghold") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{3}, {T}: Add {B} for each basic Swamp you control."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddColorlessManaEffect(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        effect = Effects.AddMana(
            Color.BLACK,
            DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.BasicLand.withSubtype("Swamp")
            ).count()
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "238"
        artist = "Dimitar Marinski"
        flavorText = "The seat of Belzenlok's power, the Stronghold serves as the gathering place for the Cabal as their dark influence spreads from Urborg."
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0bda51ef-ee3e-48d4-92e2-c9083bbe0f80.jpg?1562731196"
    }
}
