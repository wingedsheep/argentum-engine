package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Heliod's Pilgrim
 * {2}{W}
 * Creature — Human Cleric
 * 1/2
 * When this creature enters, you may search your library for an Aura card, reveal it, put it into
 * your hand, then shuffle.
 *
 * Totem-Guide Hartebeest's optional Aura tutor. The filter is the Aura subtype, so a bestow
 * enchantment creature (not an Aura card in the library) can't be found.
 */
val HeliodsPilgrim = card("Heliod's Pilgrim") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 2
    oracleText = "When this creature enters, you may search your library for an Aura card, reveal it, put it into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any.withSubtype(Subtype.AURA),
            destination = SearchDestination.HAND,
            reveal = true
        )
        description = "When this creature enters, you may search your library for an Aura card, " +
            "reveal it, put it into your hand, then shuffle."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "14"
        artist = "Izzy"
        flavorText = "The blessings of Heliod are apparent for all to see."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7ea54b97-9182-4d46-9d70-3cc7f9b18ada.jpg?1783939202"
        ruling("2014-07-18", "Enchantment creatures with bestow from the Theros block aren't Aura cards in your library. You can't find one with Heliod's Pilgrim.")
    }
}
