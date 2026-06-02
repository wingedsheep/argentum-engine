package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.dsl.HandPatterns

/**
 * Thran Temporal Gateway
 * {4}
 * Legendary Artifact
 * {4}, {T}: You may put a historic permanent card from your hand onto the battlefield.
 * (Artifacts, legendaries, and Sagas are historic.)
 *
 * Rulings:
 * 2018-04-27: A card, spell, or permanent is historic if it has the legendary supertype,
 * the artifact card type, or the Saga subtype.
 */
val ThranTemporalGateway = card("Thran Temporal Gateway") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Legendary Artifact"
    oracleText = "{4}, {T}: You may put a historic permanent card from your hand onto the battlefield. (Artifacts, legendaries, and Sagas are historic.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = HandPatterns.putFromHand(
            filter = GameObjectFilter.Historic and GameObjectFilter.Permanent
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "233"
        artist = "Jason Felix"
        flavorText = "The portal opens not to the past, but from it. Those who step through discover an unimaginable future."
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72806f4f-74e8-4621-92b0-5c63bb898884.jpg?1562737720"
        ruling("2018-04-27", "A card, spell, or permanent is historic if it has the legendary supertype, the artifact card type, or the Saga subtype.")
    }
}
