package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Dimir Cluestone
 * {3}
 * Artifact
 *
 * {T}: Add {U} or {B}.
 * {U}{B}, {T}, Sacrifice this artifact: Draw a card.
 */
val DimirCluestone = card("Dimir Cluestone") {
    manaCost = "{3}"
    colorIdentity = "UB"
    typeLine = "Artifact"
    oracleText = "{T}: Add {U} or {B}.\n" +
        "{U}{B}, {T}, Sacrifice this artifact: Draw a card."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{U}{B}"),
            Costs.Tap,
            Costs.SacrificeSelf,
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Raoul Vitale"
        flavorText = "It waits in the chill of the undercity, holding nameless secrets for those who dare to touch it."
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d8ac24f-3309-453a-b2d6-6363df9a1ddd.jpg"
    }
}
