package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Whirlermaker
 * {3}
 * Artifact
 * {4}, {T}: Create a 1/1 colorless Thopter artifact creature token with flying.
 */
val Whirlermaker = card("Whirlermaker") {
    manaCost = "{3}"
    typeLine = "Artifact"
    oracleText = "{4}, {T}: Create a 1/1 colorless Thopter artifact creature token with flying."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{4}"), Costs.Tap)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING),
            artifactToken = true,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "239"
        artist = "Victor Adame Minguez"
        flavorText = "\"Our creations are more than mere things. They have life in them, little bits of ourselves.\"\n—Saheeli Rai"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff1a1246-d5a0-43dc-825a-062a3bb4def9.jpg"
    }
}
