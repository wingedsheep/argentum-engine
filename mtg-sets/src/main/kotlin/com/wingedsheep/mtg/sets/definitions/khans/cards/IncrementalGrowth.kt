package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Incremental Growth
 * {3}{G}{G}
 * Sorcery
 * Put a +1/+1 counter on target creature, two +1/+1 counters on another target creature,
 * and three +1/+1 counters on a third target creature.
 */
val IncrementalGrowth = card("Incremental Growth") {
    manaCost = "{3}{G}{G}"
    typeLine = "Sorcery"
    oracleText = "Put a +1/+1 counter on target creature, two +1/+1 counters on another target creature, and three +1/+1 counters on a third target creature."

    spell {
        val first = target("first creature", TargetCreature())
        val second = target("second creature", TargetCreature())
        val third = target("third creature", TargetCreature())
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, first)
            .then(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, second))
            .then(Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 3, third))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "138"
        artist = "Clint Cearley"
        flavorText = "\"The bonds of family cross the boundaries of race.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcc7d833-f71b-4ec4-aad1-07b7583bad64.jpg?1562792751"
    }
}
