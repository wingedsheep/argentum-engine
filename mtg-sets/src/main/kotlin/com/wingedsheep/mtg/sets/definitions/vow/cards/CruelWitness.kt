package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Cruel Witness
 * {2}{U}{U}
 * Creature — Bird Horror
 * 3/3
 * Flying
 * Whenever you cast a noncreature spell, surveil 1.
 */
val CruelWitness = card("Cruel Witness") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird Horror"
    oracleText = "Flying\nWhenever you cast a noncreature spell, surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"
    power = 3
    toughness = 3
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = Patterns.Library.surveil(1)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Vincent Proce"
        flavorText = "\"I escaped, so why do I still feel like I'm being watched?\"\n—Gregel, militia leader"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5bf2c686-efb0-46c7-b34e-c77987914b96.jpg?1782703154"
    }
}
