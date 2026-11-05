package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Sun-Blessed Mount
 * {3}{R}{W}
 * Creature — Dinosaur
 * 4/4
 * When this creature enters, you may search your library and/or graveyard for a card named Huatli,
 * Dinosaur Knight, reveal it, then put it into your hand. If you search your library this way, shuffle.
 *
 * The planeswalker-deck tutor shape (Ashiok's Forerunner):
 * [Patterns.Library.searchLibraryAndOrGraveyard] over a name filter, optional, revealed, to hand.
 */
val SunBlessedMount = card("Sun-Blessed Mount") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Dinosaur"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, you may search your library and/or graveyard for a card " +
        "named Huatli, Dinosaur Knight, reveal it, then put it into your hand. If you search your " +
        "library this way, shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Huatli, Dinosaur Knight"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
        description = "When this creature enters, you may search your library and/or graveyard for a " +
            "card named Huatli, Dinosaur Knight, reveal it, then put it into your hand. If you search " +
            "your library this way, shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "288"
        artist = "Joe Slucher"
        flavorText = "When Huatli chooses a mount, its eyes glow with the flames of the sun."
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f06ac1f-0bae-4aaa-9ff4-28c806828c40.jpg?1783935682"
    }
}
