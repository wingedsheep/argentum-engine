package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Swiftgear Drake
 * {5}
 * Artifact Creature — Drake
 * 2/4
 * Flying, haste
 * When this creature enters, put up to one target card from a graveyard on the bottom of its
 * owner's library.
 *
 * The bottom-of-library move is Phyrexian Archivist's spelling; "up to one" is an optional target
 * (Immersturm Predator).
 */
val SwiftgearDrake = card("Swiftgear Drake") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Drake"
    power = 2
    toughness = 4
    oracleText = "Flying, haste\n" +
        "When this creature enters, put up to one target card from a graveyard on the bottom of its owner's library."

    keywords(Keyword.FLYING, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val cardInGraveyard = target(TargetFilter.CardInGraveyard, optional = true)
        effect = Effects.Move(
            target = cardInGraveyard,
            destination = Zone.LIBRARY,
            placement = ZonePlacement.Bottom
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "251"
        artist = "Zoltan Boros"
        flavorText = "Inspired by fishing raptors, Tawnos designed the drake's talons to seize prey with " +
            "overwhelming speed and precision."
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d5ae38a2-2f95-4750-9b59-53eb1771e5ab.jpg?1783920011"
    }
}
