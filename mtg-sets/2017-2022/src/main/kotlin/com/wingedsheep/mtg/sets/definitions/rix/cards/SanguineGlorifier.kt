package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sanguine Glorifier
 * {3}{W}
 * Creature — Vampire Cleric
 * 3/3
 *
 * When this creature enters, put a +1/+1 counter on another target Vampire you control.
 */
val SanguineGlorifier = card("Sanguine Glorifier") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Vampire Cleric"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, put a +1/+1 counter on another target Vampire you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Permanent.withSubtype("Vampire").youControl().other())
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "20"
        artist = "Sara Winters"
        flavorText = "For the Legion of Dusk, the Immortal Sun is a source of eternal life. When they reclaim it, they will no longer be forced to subsist on the blood of the wicked."
        imageUri = "https://cards.scryfall.io/normal/front/1/2/12993e53-8790-4d6f-9da1-a259f67aa5bf.jpg?1783935332"
    }
}
