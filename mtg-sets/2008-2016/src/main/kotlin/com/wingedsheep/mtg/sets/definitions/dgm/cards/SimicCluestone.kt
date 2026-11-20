package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Simic Cluestone
 * {3}
 * Artifact
 *
 * {T}: Add {G} or {U}.
 * {G}{U}, {T}, Sacrifice this artifact: Draw a card.
 */
val SimicCluestone = card("Simic Cluestone") {
    manaCost = "{3}"
    colorIdentity = "GU"
    typeLine = "Artifact"
    oracleText = "{T}: Add {G} or {U}.\n" +
        "{G}{U}, {T}, Sacrifice this artifact: Draw a card."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{G}{U}"),
            Costs.Tap,
            Costs.SacrificeSelf,
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "145"
        artist = "Raoul Vitale"
        flavorText = "Simic symbols are among the most intricate and fragile, yet few vandalize them for fear of the strange lifeforms that might be released."
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3c47552-afed-463d-bd24-13eb1cd724fc.jpg"
    }
}
