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
 * Academy Drake
 * {2}{U}
 * Creature — Drake
 * 2/2
 * Kicker {4}
 * Flying
 * If this creature was kicked, it enters with two +1/+1 counters on it.
 */
val AcademyDrake = card("Academy Drake") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Drake"
    power = 2
    toughness = 2
    oracleText = "Kicker {4}\nFlying\nIf this creature was kicked, it enters with two +1/+1 counters on it."

    keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{4}")))
    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8bacb12-da46-4b00-804f-9ff6bff452bc.jpg?1562745962"
    }
}
