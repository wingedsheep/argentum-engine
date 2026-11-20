package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Izzet Cluestone
 * {3}
 * Artifact
 *
 * {T}: Add {U} or {R}.
 * {U}{R}, {T}, Sacrifice this artifact: Draw a card.
 */
val IzzetCluestone = card("Izzet Cluestone") {
    manaCost = "{3}"
    colorIdentity = "UR"
    typeLine = "Artifact"
    oracleText = "{T}: Add {U} or {R}.\n" +
        "{U}{R}, {T}, Sacrifice this artifact: Draw a card."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
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
            Costs.Mana("{U}{R}"),
            Costs.Tap,
            Costs.SacrificeSelf,
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "141"
        artist = "Raoul Vitale"
        flavorText = "It holds within it an unsolvable riddle. A creative answer yields an invitation to the guild."
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8cf63def-e2cc-48c7-8409-c08a36eddf93.jpg"
    }
}
