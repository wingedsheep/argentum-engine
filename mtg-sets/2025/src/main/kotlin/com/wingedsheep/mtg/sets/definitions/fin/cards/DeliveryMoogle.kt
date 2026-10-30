package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Delivery Moogle
 * {3}{W}
 * Creature — Moogle
 * 3/2
 * Flying
 * When this creature enters, search your library and/or graveyard for an artifact card
 * with mana value 2 or less, reveal it, and put it into your hand. If you search your
 * library this way, shuffle.
 */
val DeliveryMoogle = card("Delivery Moogle") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Moogle"
    power = 3
    toughness = 2
    oracleText = "Flying\n" +
        "When this creature enters, search your library and/or graveyard for an artifact card " +
        "with mana value 2 or less, reveal it, and put it into your hand. If you search your " +
        "library this way, shuffle."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Artifact.manaValueAtMost(2),
            destination = SearchDestination.HAND,
            reveal = true,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Joseph Weston"
        flavorText = "\"A new letter has arrived just for you, kupo!\""
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f58840dc-c641-4092-8b67-9c0d449af715.jpg?1748705812"
    }
}
