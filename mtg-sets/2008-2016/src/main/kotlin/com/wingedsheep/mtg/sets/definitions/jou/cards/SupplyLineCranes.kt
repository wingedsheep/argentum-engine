package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Supply-Line Cranes
 * {3}{W}{W}
 * Creature — Bird
 * 2/4
 *
 * Flying
 * When this creature enters, put a +1/+1 counter on target creature.
 */
val SupplyLineCranes = card("Supply-Line Cranes") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    power = 2
    toughness = 4
    oracleText = "Flying\n" +
        "When this creature enters, put a +1/+1 counter on target creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "28"
        artist = "Daniel Ljunggren"
        flavorText = "They are trained from hatching to deliver aid where it is needed most."
        imageUri = "https://cards.scryfall.io/normal/front/7/6/764bd86e-4156-4c58-9ce0-ca3dffbc7298.jpg?1783939452"
    }
}
