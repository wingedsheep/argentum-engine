package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Ondu Giant
 * {3}{G}
 * Creature — Giant Druid
 * 2/4
 * When this creature enters, you may search your library for a basic land card, put it onto the
 * battlefield tapped, then shuffle.
 *
 * An optional `Triggers.self.enters()` over [Patterns.Library.searchLibrary] for a basic land to
 * the battlefield tapped — the Farhaven Elf shape.
 */
val OnduGiant = card("Ondu Giant") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Giant Druid"
    oracleText = "When this creature enters, you may search your library for a basic land card, put it onto the battlefield tapped, then shuffle."
    power = 2
    toughness = 4

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "202"
        artist = "Igor Kieryluk"
        flavorText = "Some druids nurture gardens. Others nurture continents."
        imageUri = "https://cards.scryfall.io/normal/front/d/a/daf8b1e5-418f-4e64-a2ac-0e03b387ed33.jpg?1783941961"
    }
}
