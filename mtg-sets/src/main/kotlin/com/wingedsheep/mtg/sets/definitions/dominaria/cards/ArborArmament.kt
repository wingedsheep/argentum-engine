package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Arbor Armament
 * {G}
 * Instant
 * Put a +1/+1 counter on target creature. That creature gains reach until end of turn.
 */
val ArborArmament = card("Arbor Armament") {
    manaCost = "{G}"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on target creature. That creature gains reach until end of turn."

    spell {
        val t = target("target", Targets.Creature)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, t)
            .then(Effects.GrantKeyword(Keyword.REACH, t))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "155"
        artist = "Bayard Wu"
        flavorText = "\"Llanowar's boughs are ever ready / To unleash an autumn of steel leaves.\" —\"Song of Freyalise\""
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bceb365c-5de6-47ae-b42d-7fbce7781f8e.jpg?1615334447"
    }
}
