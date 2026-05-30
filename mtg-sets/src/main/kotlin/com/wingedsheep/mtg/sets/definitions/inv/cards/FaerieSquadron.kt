package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Faerie Squadron
 * {U}
 * Creature — Faerie
 * 1/1
 * Kicker {3}{U} (You may pay an additional {3}{U} as you cast this spell.)
 * If this creature was kicked, it enters with two +1/+1 counters on it and with flying.
 */
val FaerieSquadron = card("Faerie Squadron") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Faerie"
    power = 1
    toughness = 1
    oracleText = "Kicker {3}{U} (You may pay an additional {3}{U} as you cast this spell.)\n" +
        "If this creature was kicked, it enters with two +1/+1 counters on it and with flying."

    keywordAbility(KeywordAbility.kicker("{3}{U}"))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self),
            Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self, Duration.Permanent)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "58"
        artist = "rk post"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c707c81-dbbd-43be-a79a-7bc92a584839.jpg?1562910474"
    }
}
