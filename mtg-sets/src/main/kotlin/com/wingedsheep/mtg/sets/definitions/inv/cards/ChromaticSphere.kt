package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Chromatic Sphere
 * {1}
 * Artifact
 *
 * {1}, {T}, Sacrifice this artifact: Add one mana of any color. Draw a card.
 */
val ChromaticSphere = card("Chromatic Sphere") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{1}, {T}, Sacrifice this artifact: Add one mana of any color. Draw a card."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Composite(
            Effects.AddAnyColorMana(1),
            Effects.DrawCards(1),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "299"
        artist = "Mark Zug"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/920cd17f-9274-443e-906f-c9904f0658d5.jpg?1562924494"
        ruling(
            "2008-08-01",
            "This is a mana ability, which means it can be activated as part of the process of casting a spell or activating another ability. If that happens you get the mana right away, but you don't get to look at the drawn card until you have finished casting that spell or activating that ability.",
        )
    }
}
