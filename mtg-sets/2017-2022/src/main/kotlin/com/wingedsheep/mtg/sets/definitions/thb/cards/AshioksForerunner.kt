package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Ashiok's Forerunner
 * {3}{U}{B}
 * Creature — Human Wizard
 * 3/3
 * Flash
 * When this creature enters, you may search your library and/or graveyard for a card named Ashiok,
 * Sculptor of Fears, reveal it, and put it into your hand. If you search your library this way,
 * shuffle.
 *
 * The planeswalker-deck tutor shape of Chandra's Firemaw:
 * [Patterns.Library.searchLibraryAndOrGraveyard] over a name filter, optional, revealed, to hand.
 */
val AshioksForerunner = card("Ashiok's Forerunner") {
    manaCost = "{3}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Human Wizard"
    power = 3
    toughness = 3
    oracleText = "Flash\n" +
        "When this creature enters, you may search your library and/or graveyard for a card named " +
        "Ashiok, Sculptor of Fears, reveal it, and put it into your hand. If you search your library " +
        "this way, shuffle."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Ashiok, Sculptor of Fears"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
        description = "When this creature enters, you may search your library and/or graveyard for a " +
            "card named Ashiok, Sculptor of Fears, reveal it, and put it into your hand. If you search " +
            "your library this way, shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "277"
        artist = "Johannes Voss"
        flavorText = "\"Every dream is a doorway.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/8/781bfbae-acd9-4583-88d2-d31978eef565.jpg?1783931498"
    }
}
