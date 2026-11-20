package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Rakdos Cluestone
 * {3}
 * Artifact
 *
 * {T}: Add {B} or {R}.
 * {B}{R}, {T}, Sacrifice this artifact: Draw a card.
 */
val RakdosCluestone = card("Rakdos Cluestone") {
    manaCost = "{3}"
    colorIdentity = "BR"
    typeLine = "Artifact"
    oracleText = "{T}: Add {B} or {R}.\n" +
        "{B}{R}, {T}, Sacrifice this artifact: Draw a card."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{B}{R}"),
            Costs.Tap,
            Costs.SacrificeSelf,
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "143"
        artist = "Raoul Vitale"
        flavorText = "\"Burn. Bleed. Enjoy.\"\n—Cluestone inscription"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9ef43817-1813-4608-8e3d-3c14321ab736.jpg"
    }
}
