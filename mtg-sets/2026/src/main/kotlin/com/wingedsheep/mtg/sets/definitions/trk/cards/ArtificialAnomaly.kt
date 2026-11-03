package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Artificial Anomaly
 * Land
 *
 * {T}: Add {C}.
 * {3}{B}, {T}, Sacrifice this land: Each opponent loses 2 life and you gain 2 life. Create a tapped
 * Planet land token named New Planet with "{T}: Add one mana of any color."
 */
val ArtificialAnomaly = card("Artificial Anomaly") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{3}{B}, {T}, Sacrifice this land: Each opponent loses 2 life and you gain 2 life. Create a tapped Planet land token named New Planet with \"{T}: Add one mana of any color.\""
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{B}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(2) then
            Effects.CreateNewPlanet()
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "276"
        artist = "Jonas De Ro"
        flavorText = "\"The object's enormous mass is causing a great deal of gravimetric interference.\"\n—Lieutenant Commander Data"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7dfead74-0625-4f6a-b66b-6d5d363a387c.jpg?1793273918"
    }
}
