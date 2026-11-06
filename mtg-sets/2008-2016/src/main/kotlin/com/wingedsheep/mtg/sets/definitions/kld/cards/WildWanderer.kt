package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Wild Wanderer
 * {3}{G}
 * Creature — Elf Druid
 * 3/2
 * When this creature enters, you may search your library for a basic land card, put it onto the battlefield tapped, then shuffle.
 */
val WildWanderer = card("Wild Wanderer") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Druid"
    oracleText = "When this creature enters, you may search your library for a basic land card, put it onto the battlefield tapped, then shuffle."
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.May(
            Patterns.Library.searchLibrary(
                filter = GameObjectFilter.BasicLand,
                destination = SearchDestination.BATTLEFIELD,
                entersTapped = true,
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Sidharth Chaturvedi"
        flavorText = "\"We all began in nature, and nature is where we belong.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d20e020-30e1-4deb-8dfc-4c5fe056193d.jpg?1783937172"
    }
}
