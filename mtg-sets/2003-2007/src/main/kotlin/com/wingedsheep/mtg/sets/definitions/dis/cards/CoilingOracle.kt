package com.wingedsheep.mtg.sets.definitions.dis.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Coiling Oracle {G}{U}
 * Creature — Snake Elf Druid
 * 1/1
 * When this creature enters, reveal the top card of your library. If it's a land card, put it
 * onto the battlefield. Otherwise, put that card into your hand.
 *
 * Same reveal-and-split shape as Nadu, Winged Wisdom's granted trigger.
 */
val CoilingOracle = card("Coiling Oracle") {
    manaCost = "{G}{U}"
    typeLine = "Creature — Snake Elf Druid"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, reveal the top card of your library. If it's a land card, " +
        "put it onto the battlefield. Otherwise, put that card into your hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(1))
            reveal(top, fromZone = Zone.LIBRARY)
            val (lands, nonlands) = filterSplit(top, GameObjectFilter.Land)
            move(lands, CardDestination.ToZone(Zone.BATTLEFIELD))
            toHand(nonlands)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "107"
        artist = "Mark Zug"
        flavorText = "Snaking remnants of nature directed by a body of thought and progress, the oracles embody all that is Simic."
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55a6ba2a-b372-4b15-9a1e-09b41316eab7.jpg?1783943405"
    }
}
