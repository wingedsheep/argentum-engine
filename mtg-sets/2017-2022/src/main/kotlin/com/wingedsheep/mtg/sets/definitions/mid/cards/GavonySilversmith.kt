package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gavony Silversmith (Innistrad: Midnight Hunt #21)
 * {3}{W}
 * Creature — Human Soldier 2/3
 * When this creature enters, put a +1/+1 counter on each of up to two target creatures.
 *
 * One target requirement taking up to two creatures (any controller), fanned out with
 * [Effects.ForEachTarget] — the Travel Preparations idiom.
 */
val GavonySilversmith = card("Gavony Silversmith") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, put a +1/+1 counter on each of up to two target creatures."

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        )
        description = "When this creature enters, put a +1/+1 counter on each of up to two target creatures."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "21"
        artist = "Volkan Baǵa"
        flavorText = "More than heat and the strength of his arm, it was his faith that gave the blade its unyielding edge."
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c53b007-96aa-4db6-8c3e-529d1e845b19.jpg?1783925658"
    }
}
