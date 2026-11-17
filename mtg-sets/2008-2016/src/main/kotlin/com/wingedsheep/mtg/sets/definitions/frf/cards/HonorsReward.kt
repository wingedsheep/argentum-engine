package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Honor's Reward
 * {2}{W}
 * Instant
 * You gain 4 life. Bolster 2. (Choose a creature with the least toughness among creatures you
 * control and put two +1/+1 counters on it.)
 *
 * The bolstered creature is chosen as the spell resolves; bolster doesn't target. With no
 * creatures, the life gain still happens and the bolster does nothing.
 */
val HonorsReward = card("Honor's Reward") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "You gain 4 life. Bolster 2. " +
        "(Choose a creature with the least toughness among creatures you control and put two +1/+1 counters on it.)"

    spell {
        effect = Effects.GainLife(4) then Patterns.Mechanic.bolster(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "14"
        artist = "Izzy"
        flavorText = "It seldom rains in Abzan lands. When it does, the khan marks the occasion by honoring the families of the fallen."
        imageUri = "https://cards.scryfall.io/normal/front/0/8/084c8b16-7aae-4d1f-87c2-6a150f11bb15.jpg?1783938712"
        ruling("2014-11-24", "You determine which creature to put counters on as the spell or ability that instructs you to bolster resolves.")
    }
}
