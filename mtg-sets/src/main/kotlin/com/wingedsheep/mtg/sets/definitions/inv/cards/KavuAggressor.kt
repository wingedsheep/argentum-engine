package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kavu Aggressor
 * {2}{R}
 * Creature — Kavu
 * 3/2
 * Kicker {4}
 * This creature can't block.
 * If this creature was kicked, it enters with a +1/+1 counter on it.
 */
val KavuAggressor = card("Kavu Aggressor") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Kavu"
    power = 3
    toughness = 2
    oracleText = "Kicker {4} (You may pay an additional {4} as you cast this spell.)\n" +
        "This creature can't block.\n" +
        "If this creature was kicked, it enters with a +1/+1 counter on it."

    keywordAbility(KeywordAbility.kicker("{4}"))

    staticAbility {
        ability = CantBlock()
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a2832ad3-ce7f-44d2-beb2-c95d982905a6.jpg?1562927844"
    }
}
