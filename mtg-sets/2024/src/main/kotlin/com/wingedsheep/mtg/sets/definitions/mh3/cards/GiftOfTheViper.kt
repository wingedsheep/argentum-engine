package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gift of the Viper
 * {G}
 * Instant
 *
 * Put a +1/+1 counter, a reach counter, and a deathtouch counter on target creature. Untap it.
 *
 * Reach and deathtouch counters are keyword counters, projected into the keyword set by
 * `StateProjector`, so the creature keeps both abilities for as long as the counters stay on it.
 */
val GiftOfTheViper = card("Gift of the Viper") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter, a reach counter, and a deathtouch counter on target creature. Untap it."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.AddCounters(CounterType.REACH, 1, creature) then
            Effects.AddCounters(CounterType.DEATHTOUCH, 1, creature) then
            Effects.Untap(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "156"
        artist = "Madeline Boni"
        flavorText = "Ohran mages willingly endure the bites of vipers, believing the snakes bestow great power on those who survive their venom."
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17fa0b62-d474-4dc7-9631-9d0a9d99e5e2.jpg?1783911260"
    }
}
