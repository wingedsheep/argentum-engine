package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Baloth Gorger
 * {2}{G}{G}
 * Creature — Beast
 * 4/4
 * Kicker {4}
 * If this creature was kicked, it enters with three +1/+1 counters on it.
 */
val BalothGorger = card("Baloth Gorger") {
    manaCost = "{2}{G}{G}"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "Kicker {4}\nIf this creature was kicked, it enters with three +1/+1 counters on it."

    keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{4}")))

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 3, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "156"
        artist = "Zezhou Chen"
        flavorText = "A baloth only cares about the many things it eats and the few things that eat it."
        imageUri = "https://cards.scryfall.io/normal/front/5/0/504090bb-d183-4833-aea5-d4193b5c57a1.jpg?1562735490"
    }
}
