package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped

/**
 * Spare Supplies — Zendikar Rising #254
 * {2} · Artifact
 *
 * This artifact enters tapped.
 * When this artifact enters, draw a card.
 * {2}, {T}, Sacrifice this artifact: Draw a card.
 */
val SpareSupplies = card("Spare Supplies") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "This artifact enters tapped.\n" +
        "When this artifact enters, draw a card.\n" +
        "{2}, {T}, Sacrifice this artifact: Draw a card."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
        description = "When this artifact enters, draw a card."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.DrawCards(1)
        description = "{2}, {T}, Sacrifice this artifact: Draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "254"
        artist = "Jason Felix"
        flavorText = "\"It's better to carry a little extra weight than to find yourself halfway down a " +
            "baloth's throat with no knife.\"\n—Doral, Bala Ged guide"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a53baf25-1782-427b-a9dd-fc9b8dc6444f.jpg"
    }
}
