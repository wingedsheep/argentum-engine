package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Spyglass Siren — {U}
 * Creature — Siren Pirate
 * 1/1
 * Flying
 * When this creature enters, create a Map token.
 */
val SpyglassSiren = card("Spyglass Siren") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Siren Pirate"
    oracleText = "Flying\nWhen this creature enters, create a Map token. (It's an artifact with \"{1}, {T}, Sacrifice this token: Target creature you control explores. Activate only as a sorcery.\")"
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateMapToken()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "David Astruga"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41e54343-95e5-4dc4-9f18-e4a415fe5e0a.jpg?1782694547"
    }
}
