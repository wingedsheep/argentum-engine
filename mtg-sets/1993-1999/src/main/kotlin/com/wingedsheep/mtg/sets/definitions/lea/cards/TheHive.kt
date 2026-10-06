package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * The Hive — Limited Edition Alpha #272
 * {5} · Artifact
 *
 * {5}, {T}: Create a 1/1 colorless Insect artifact creature token with flying named Wasp.
 *
 * LEA has no token cards; the Wasp's art is the most recent Wasp token printing (30th Anniversary).
 */
val TheHive = card("The Hive") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{5}, {T}: Create a 1/1 colorless Insect artifact creature token with flying named Wasp. " +
        "(It can't be blocked except by creatures with flying or reach.)"

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{5}"), Costs.Tap)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Insect"),
            keywords = setOf(Keyword.FLYING),
            name = "Wasp",
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/0/9/09921372-126f-4c81-b6d8-ea50b1d0eb44.jpg?1783919200",
        )
        description = "{5}, {T}: Create a 1/1 colorless Insect artifact creature token with flying named Wasp."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "272"
        artist = "Sandra Everingham"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/544a7138-eae8-4ff9-9e17-680bfa717183.jpg?1783948661"
    }
}
