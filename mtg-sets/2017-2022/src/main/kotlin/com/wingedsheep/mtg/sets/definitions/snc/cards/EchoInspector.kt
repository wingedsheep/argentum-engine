package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Echo Inspector
 * {3}{U}
 * Creature — Bird Rogue
 * 2 / 3
 * Flying
 * When this creature enters, it connives.
 */
val EchoInspector = card("Echo Inspector") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird Rogue"
    oracleText = "Flying\n" +
        "When this creature enters, it connives. (Draw a card, then discard a card. If you discarded a nonland card, put a +1/+1 counter on this creature.)"
    power = 2
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Connive()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40"
        artist = "Lucas Graciano"
        flavorText = "The rendezvous took place hours ago, but the Obscura agent read the residue with ease."
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d7e530ea-ba51-4f7a-bf56-b657a48e86ae.jpg"
    }
}
