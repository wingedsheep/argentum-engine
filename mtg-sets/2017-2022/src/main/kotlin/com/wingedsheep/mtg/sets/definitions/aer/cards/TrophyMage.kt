package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Trophy Mage
 * {2}{U}
 * Creature — Human Wizard
 * 2/2
 *
 * When this creature enters, you may search your library for an artifact card with mana value 3,
 * reveal it, put it into your hand, then shuffle.
 */
val TrophyMage = card("Trophy Mage") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    oracleText = "When this creature enters, you may search your library for an artifact card with mana value 3, reveal it, put it into your hand, then shuffle."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Artifact.manaValue(3),
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Anna Steinbauer"
        flavorText = "\"The tools of the past will help us forge the future.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19754fe4-2f61-42a3-afa2-3a6a8257b81b.jpg?1783936768"
        ruling("2017-02-09", "Trophy Mage's ability finds an artifact card with mana value exactly 3.")
    }
}
