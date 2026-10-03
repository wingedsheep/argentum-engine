package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Primeval Herald
 * {3}{G}
 * Creature — Elf Scout
 * 3/1
 * Trample (This creature can deal excess combat damage to the player or planeswalker it's attacking.)
 * Whenever this creature enters or attacks, you may search your library for a basic land card,
 * put it onto the battlefield tapped, then shuffle.
 *
 * "Enters or attacks" is the corpus's two-ability idiom (Lumbering Worldwagon): one
 * `Triggers.self.enters()` and one `Triggers.self.attacks()` sharing the same `Effects.May`-wrapped search.
 */
val PrimevalHerald = card("Primeval Herald") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Scout"
    oracleText = "Trample (This creature can deal excess combat damage to the player or planeswalker it's attacking.)\n" +
        "Whenever this creature enters or attacks, you may search your library for a basic land card, " +
        "put it onto the battlefield tapped, then shuffle."
    power = 3
    toughness = 1

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                count = 1,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "42"
        artist = "Tatiana Kirgetova"
        flavorText = "When they call, nature answers."
        imageUri = "https://cards.scryfall.io/normal/front/f/b/fbb0a91b-246b-4c1f-9b98-2ad6ff1ba124.jpg"
    }
}
