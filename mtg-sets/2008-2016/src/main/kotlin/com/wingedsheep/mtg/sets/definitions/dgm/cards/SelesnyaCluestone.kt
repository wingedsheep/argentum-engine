package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Selesnya Cluestone
 * {3}
 * Artifact
 *
 * {T}: Add {G} or {W}.
 * {G}{W}, {T}, Sacrifice this artifact: Draw a card.
 */
val SelesnyaCluestone = card("Selesnya Cluestone") {
    manaCost = "{3}"
    colorIdentity = "GW"
    typeLine = "Artifact"
    oracleText = "{T}: Add {G} or {W}.\n" +
        "{G}{W}, {T}, Sacrifice this artifact: Draw a card."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{G}{W}"),
            Costs.Tap,
            Costs.SacrificeSelf,
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "144"
        artist = "Raoul Vitale"
        flavorText = "\"Break it if you must. The loss of one relic is nothing next to the value of enriching us all.\"\n—Suniel the Woodwise"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/34ad5631-439a-43e2-b00a-04f78d66b8e6.jpg"
    }
}
