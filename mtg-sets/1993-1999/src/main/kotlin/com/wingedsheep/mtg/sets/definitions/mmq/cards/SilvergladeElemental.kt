package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Silverglade Elemental
 * {4}{G}
 * Creature — Elemental
 * 4 / 4
 *
 * Wood Elves' fetch under an optional ETB: [Patterns.Library.searchLibrary] supplies the gather /
 * choose-up-to-one / put-onto-battlefield / shuffle sequence (`shuffleAfter` defaults to true, which
 * is the printed "then shuffle").
 */
val SilvergladeElemental = card("Silverglade Elemental") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    oracleText = "When this creature enters, you may search your library for a Forest card, put that card onto the battlefield, then shuffle."
    power = 4
    toughness = 4

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land.withSubtype(Subtype.FOREST),
            destination = SearchDestination.BATTLEFIELD
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "269"
        artist = "Chippy"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f222fe90-ac92-4ba9-b060-9b64075bf139.jpg"
    }
}
