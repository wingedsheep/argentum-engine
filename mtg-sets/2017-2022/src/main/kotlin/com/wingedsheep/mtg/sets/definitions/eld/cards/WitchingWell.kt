package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Witching Well
 * {U}
 * Artifact
 *
 * When this artifact enters, scry 2.
 * {3}{U}, Sacrifice this artifact: Draw two cards.
 */
val WitchingWell = card("Witching Well") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, scry 2. (Look at the top two cards of your library, then put any number " +
        "of them on the bottom and the rest on top in any order.)\n" +
        "{3}{U}, Sacrifice this artifact: Draw two cards."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Scry(2)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{U}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "74"
        artist = "John Avon"
        flavorText = "It was built by the Witch of Lochmere. Most of the wishes it grants are its own."
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62d3132f-f897-4a7a-9de4-c6388e83f5ad.jpg"
    }
}
