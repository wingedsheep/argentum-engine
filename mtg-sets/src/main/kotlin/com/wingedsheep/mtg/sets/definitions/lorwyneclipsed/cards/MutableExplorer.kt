package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Mutable Explorer
 * {2}{G}
 * Creature — Shapeshifter
 * 1/1
 *
 * Changeling (This card is every creature type.)
 * When this creature enters, create a tapped Mutavault token. (It's a land with
 * "{T}: Add {C}" and "{1}: This token becomes a 2/2 creature with all creature
 * types until end of turn. It's still a land.")
 */
val MutableExplorer = card("Mutable Explorer") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Shapeshifter"
    power = 1
    toughness = 1
    oracleText = "Changeling (This card is every creature type.)\n" +
        "When this creature enters, create a tapped Mutavault token. " +
        "(It's a land with \"{T}: Add {C}\" and \"{1}: This token becomes a 2/2 creature " +
        "with all creature types until end of turn. It's still a land.\")"

    keywords(Keyword.CHANGELING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateMutavault(tapped = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "186"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f35d95a-caea-4d5e-b98e-55da1ba7c92d.jpg?1759120542"
        ruling("2025-11-17", "Mutable Explorer's last ability creates a token that's a copy of the card Mutavault in the Oracle card reference.")
    }
}
