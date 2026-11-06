package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Shefet Monitor
 * {5}{G}
 * Creature — Lizard
 * 6/5
 *
 * Cycling {3}{G}
 * When you cycle this card, you may search your library for a basic land card or a Desert card,
 * put it onto the battlefield, then shuffle.
 *
 * The cycle trigger goes on the stack above the cycling ability, so the land arrives before the draw.
 */
val ShefetMonitor = card("Shefet Monitor") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Lizard"
    power = 6
    toughness = 5
    oracleText = "Cycling {3}{G} ({3}{G}, Discard this card: Draw a card.)\n" +
        "When you cycle this card, you may search your library for a basic land card or a Desert " +
        "card, put it onto the battlefield, then shuffle. (Do this before you draw.)"

    keywordAbility(KeywordAbility.cycling("{3}{G}"))

    triggeredAbility {
        trigger = Triggers.self.isCycled()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand or GameObjectFilter.Land.withSubtype(Subtype.DESERT),
            count = 1,
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "186"
        artist = "Viktor Titov"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/2688ef62-875a-4735-9a63-6f3152d10d61.jpg?1783936469"
        ruling("2022-12-08", "When you cycle this card, first the cycling ability goes on the stack, then the triggered ability goes on the stack on top of it. The triggered ability will resolve before you draw a card from the cycling ability.")
        ruling("2022-12-08", "The cycling ability and the triggered ability are separate. If the triggered ability doesn't resolve (because, for example, it has been countered, or all of its targets have become illegal), the cycling ability will still resolve, and you'll draw a card.")
    }
}
