package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Hunting Cheetah
 * {2}{G}
 * Creature — Cat
 * 2/3
 * Whenever this creature deals damage to an opponent, you may search your library for a Forest card,
 * reveal that card, put it into your hand, then shuffle.
 */
val HuntingCheetah = card("Hunting Cheetah") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat"
    power = 2
    toughness = 3
    oracleText = "Whenever this creature deals damage to an opponent, you may search your library for a Forest card, reveal that card, put it into your hand, then shuffle."

    triggeredAbility {
        trigger = Triggers.self.dealsDamage(Recipient.Opponent)
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land.withSubtype(Subtype.FOREST),
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Fang Yue"
        imageUri = "https://cards.scryfall.io/normal/front/5/6/56a63628-17e6-4845-96b4-c82c3e7e8fb5.jpg?1783946101"
    }
}
