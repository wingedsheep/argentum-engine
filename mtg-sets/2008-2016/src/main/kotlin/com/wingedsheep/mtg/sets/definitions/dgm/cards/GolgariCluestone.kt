package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Golgari Cluestone
 * {3}
 * Artifact
 *
 * {T}: Add {B} or {G}.
 * {B}{G}, {T}, Sacrifice this artifact: Draw a card.
 */
val GolgariCluestone = card("Golgari Cluestone") {
    manaCost = "{3}"
    colorIdentity = "BG"
    typeLine = "Artifact"
    oracleText = "{T}: Add {B} or {G}.\n" +
        "{B}{G}, {T}, Sacrifice this artifact: Draw a card."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{B}{G}"),
            Costs.Tap,
            Costs.SacrificeSelf,
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Raoul Vitale"
        flavorText = "Golgari mold collects whispers, intelligence that is later harvested by Jarad and his high chancellors."
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff77e1ee-7fa3-4370-a0c9-ec008b63302f.jpg"
    }
}
