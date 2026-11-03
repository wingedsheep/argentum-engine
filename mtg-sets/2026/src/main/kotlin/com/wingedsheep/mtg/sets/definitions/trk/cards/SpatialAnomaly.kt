package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spatial Anomaly
 * Land
 *
 * {T}: Add {C}.
 * {4}{R}, {T}, Sacrifice this land: It deals 2 damage to target creature. Create a tapped Planet
 * land token named New Planet with "{T}: Add one mana of any color."
 */
val SpatialAnomaly = card("Spatial Anomaly") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{4}{R}, {T}, Sacrifice this land: It deals 2 damage to target creature. Create a tapped Planet land token named New Planet with \"{T}: Add one mana of any color.\""
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}{R}"), Costs.Tap, Costs.SacrificeSelf)
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(2, creature) then Effects.CreateNewPlanet()
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "296"
        artist = "Pablo Mendoza"
        flavorText = "\"A four-dimensional incursion? Fascinating.\"\n—Mr. Spock"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f683f37-4291-410d-b179-ed6139c06877.jpg?1793108360"
    }
}
