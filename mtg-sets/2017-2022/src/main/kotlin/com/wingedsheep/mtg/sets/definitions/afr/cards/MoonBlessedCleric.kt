package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Moon-Blessed Cleric
 * {2}{W}
 * Creature — Human Elf Cleric
 * 3/2
 * Divine Intervention — When this creature enters, you may search your library for an
 * enchantment card, reveal it, then shuffle and put that card on top.
 */
val MoonBlessedCleric = card("Moon-Blessed Cleric") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Elf Cleric"
    oracleText = "Divine Intervention — When this creature enters, you may search your library for an enchantment card, reveal it, then shuffle and put that card on top."
    power = 3
    toughness = 2
    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Enchantment,
            destination = SearchDestination.TOP_OF_LIBRARY,
            reveal = true
        )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Marta Nael"
        flavorText = "Selûne guides her devotees with majestic wisdom and grace."
        imageUri = "https://cards.scryfall.io/normal/front/2/b/2b7fea79-9600-41d2-84d2-34a61a08a0d4.jpg?1783926528"
    }
}
