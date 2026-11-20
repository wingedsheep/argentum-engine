package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Orzhov Cluestone
 * {3}
 * Artifact
 *
 * {T}: Add {W} or {B}.
 * {W}{B}, {T}, Sacrifice this artifact: Draw a card.
 */
val OrzhovCluestone = card("Orzhov Cluestone") {
    manaCost = "{3}"
    colorIdentity = "WB"
    typeLine = "Artifact"
    oracleText = "{T}: Add {W} or {B}.\n" +
        "{W}{B}, {T}, Sacrifice this artifact: Draw a card."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
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
            Costs.Mana("{W}{B}"),
            Costs.Tap,
            Costs.SacrificeSelf,
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "142"
        artist = "Raoul Vitale"
        flavorText = "A symbol of power and wealth, the only commodities that matter to the Orzhov."
        imageUri = "https://cards.scryfall.io/normal/front/4/8/4823f904-1c41-42cf-aef7-db0dcf82b10b.jpg"
    }
}
