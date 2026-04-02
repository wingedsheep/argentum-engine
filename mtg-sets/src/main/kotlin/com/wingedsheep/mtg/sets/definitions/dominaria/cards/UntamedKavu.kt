package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Untamed Kavu
 * {1}{G}
 * Creature — Kavu
 * 2/2
 * Kicker {3}
 * Vigilance, trample
 * If this creature was kicked, it enters with three +1/+1 counters on it.
 */
val UntamedKavu = card("Untamed Kavu") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Kavu"
    power = 2
    toughness = 2
    oracleText = "Kicker {3}\nVigilance, trample\nIf this creature was kicked, it enters with three +1/+1 counters on it."

    keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{3}")))
    keywords(Keyword.VIGILANCE, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 3, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "186"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c4cf84a-2024-45bb-9e24-8a9a6d9ad247.jpg?1562734310"
    }
}
