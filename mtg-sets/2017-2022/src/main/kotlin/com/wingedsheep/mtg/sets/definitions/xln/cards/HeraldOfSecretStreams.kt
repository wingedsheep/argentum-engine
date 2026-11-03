package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Herald of Secret Streams
 * {3}{U}
 * Creature — Merfolk Warrior
 * 2/3
 * Creatures you control with +1/+1 counters on them can't be blocked.
 */
val HeraldOfSecretStreams = card("Herald of Secret Streams") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Warrior"
    power = 2
    toughness = 3
    oracleText = "Creatures you control with +1/+1 counters on them can't be blocked."

    staticAbility {
        ability = CantBeBlocked(
            GroupFilter(GameObjectFilter.Creature.youControl().withCounter(CounterType.PLUS_ONE_PLUS_ONE))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "59"
        artist = "Sara Winters"
        flavorText = "\"You might as well try to stop the waterfall.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/6/168cbca9-0124-478b-8dd7-0bd9dc711c6d.jpg?1783935781"
    }
}
