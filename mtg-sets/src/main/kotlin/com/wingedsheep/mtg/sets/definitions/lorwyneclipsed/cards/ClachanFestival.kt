package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Clachan Festival
 * {2}{W}
 * Kindred Enchantment — Kithkin
 *
 * When this enchantment enters, create two 1/1 green and white Kithkin creature tokens.
 * {4}{W}: Create a 1/1 green and white Kithkin creature token.
 */
val ClachanFestival = card("Clachan Festival") {
    manaCost = "{2}{W}"
    typeLine = "Kindred Enchantment — Kithkin"
    oracleText = "When this enchantment enters, create two 1/1 green and white Kithkin creature tokens.\n" +
        "{4}{W}: Create a 1/1 green and white Kithkin creature token."

    // When this enchantment enters, create two 1/1 green and white Kithkin creature tokens.
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf("Kithkin"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/2/e/2ed11e1b-2289-48d2-8d96-ee7e590ecfd4.jpg?1767955680"
        )
    }

    // {4}{W}: Create a 1/1 green and white Kithkin creature token.
    activatedAbility {
        cost = Costs.Mana("{4}{W}")
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf("Kithkin"),
            imageUri = "https://cards.scryfall.io/normal/front/2/e/2ed11e1b-2289-48d2-8d96-ee7e590ecfd4.jpg?1767955680"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "Kev Fang"
        flavorText = "Unmatched in their harmony, the troupe took home the festival's trophy with a stirring rendition of \"Dundoolin Downs.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/2/324b5234-ffbf-4801-a475-8f693679ae2f.jpg?1767871685"
    }
}
