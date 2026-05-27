package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Wizard's Rockets
 * {1}
 * Artifact
 *
 * This artifact enters tapped.
 * {X}, {T}, Sacrifice this artifact: Add X mana in any combination of colors.
 * When this artifact is put into a graveyard from the battlefield, draw a card.
 */
val WizardsRockets = card("Wizard's Rockets") {
    manaCost = "{1}"
    typeLine = "Artifact"
    oracleText = "This artifact enters tapped.\n" +
        "{X}, {T}, Sacrifice this artifact: Add X mana in any combination of colors.\n" +
        "When this artifact is put into a graveyard from the battlefield, draw a card."

    // This artifact enters tapped.
    replacementEffect(EntersTapped())

    // {X}, {T}, Sacrifice this artifact: Add X mana in any combination of colors.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.AddAnyColorMana(DynamicAmount.XValue)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // When this artifact is put into a graveyard from the battlefield, draw a card.
    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "252"
        artist = "Yuriy Chemezov"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c6ed742-dfb1-41e2-8f19-184555109e34.jpg?1686970310"
    }
}
