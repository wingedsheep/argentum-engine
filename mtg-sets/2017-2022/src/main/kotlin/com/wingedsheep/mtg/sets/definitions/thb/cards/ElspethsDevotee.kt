package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Elspeth's Devotee
 * {2}{W}{W}
 * Creature — Human Soldier
 * 3/3
 * When this creature enters, you may search your library and/or graveyard for a card named
 * Elspeth, Undaunted Hero, reveal it, and put it into your hand. If you search your library this
 * way, shuffle.
 *
 * Teferi's Wavecaster's [Patterns.Library.searchLibraryAndOrGraveyard], naming Elspeth.
 */
val ElspethsDevotee = card("Elspeth's Devotee") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, you may search your library and/or graveyard for a card " +
        "named Elspeth, Undaunted Hero, reveal it, and put it into your hand. If you search your " +
        "library this way, shuffle."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Elspeth, Undaunted Hero"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
        description = "When this creature enters, you may search your library and/or graveyard for a " +
            "card named Elspeth, Undaunted Hero, reveal it, and put it into your hand. If you search " +
            "your library this way, shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "272"
        artist = "Caroline Gariba"
        flavorText = "\"For her, I would gladly die again.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f82064c-b335-454c-9be6-9fbcb21aa8e5.jpg?1783931499"
    }
}
