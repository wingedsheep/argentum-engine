package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Guild Globe
 * {2}
 * Artifact
 *
 * When this artifact enters, draw a card.
 * {2}, {T}, Sacrifice this artifact: Add two mana of different colors.
 */
val GuildGlobe = card("Guild Globe") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, draw a card.\n" +
        "{2}, {T}, Sacrifice this artifact: Add two mana of different colors."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.AddManaOfDifferentColors(2)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "239"
        artist = "Daniel Ljunggren"
        flavorText = "\"Hopefully Ravnica will prove harder to break.\"\n—Karn"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7faf99e-f61f-46cb-a275-2e12c41f1e31.jpg?1783933374"
    }
}
