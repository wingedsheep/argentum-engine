package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Golden Egg
 * {2}
 * Artifact — Food
 *
 * When this artifact enters, draw a card.
 * {1}, {T}, Sacrifice this artifact: Add one mana of any color.
 * {2}, {T}, Sacrifice this artifact: You gain 3 life.
 */
val GoldenEgg = card("Golden Egg") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Food"
    oracleText = "When this artifact enters, draw a card.\n" +
        "{1}, {T}, Sacrifice this artifact: Add one mana of any color.\n" +
        "{2}, {T}, Sacrifice this artifact: You gain 3 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.AddManaOfChoice()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.GainLife(3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "220"
        artist = "Lindsey Look"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/525142c3-f17c-4e02-a02d-fa385215aa12.jpg?1783932586"
    }
}
