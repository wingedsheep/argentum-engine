package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Infuse with the Elements
 * {3}{G}
 * Instant
 * Converge — Put X +1/+1 counters on target creature, where X is the number of colors of mana
 * spent to cast this spell. That creature gains trample until end of turn.
 *
 * Converge: X = [DynamicAmounts.colorsOfManaSpent], read while the spell resolves (the Snarl Song
 * shape). The trample grant is unconditional, so it still applies when X is zero (e.g. a copy).
 */
val InfuseWithTheElements = card("Infuse with the Elements") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Converge — Put X +1/+1 counters on target creature, where X is the number of colors " +
        "of mana spent to cast this spell. That creature gains trample until end of turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.colorsOfManaSpent(),
            t
        ) then Effects.GrantKeyword(Keyword.TRAMPLE, target = t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "Daniel Ljunggren"
        flavorText = "\"Zendikar has given us the weapons to wage war on the appropriate scale.\"\n—Najiya, leader of the Tajuru"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0ec25fe4-7967-42e2-8790-593738991eaa.jpg?1783938188"
    }
}
