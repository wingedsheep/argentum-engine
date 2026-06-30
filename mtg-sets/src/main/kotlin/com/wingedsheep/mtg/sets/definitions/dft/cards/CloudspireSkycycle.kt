package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Cloudspire Skycycle — Aetherdrift #197
 * {2}{R}{W} · Artifact — Vehicle · 2/3
 *
 * Flying
 * When this Vehicle enters, distribute two +1/+1 counters among one or two other target Vehicles
 * and/or creatures you control.
 * Crew 1
 *
 * "One or two other target Vehicles and/or creatures you control" is a 1-to-2-count permanent
 * target ([TargetObject] with `count = 2`, `minCount = 1`) over [GameObjectFilter.CreatureOrVehicle]
 * you control with `excludeSelf` ("other"). [Effects.DistributeCountersAmongTargets] splits the two
 * +1/+1 counters across the chosen targets (min one per target).
 */
val CloudspireSkycycle = card("Cloudspire Skycycle") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Artifact — Vehicle"
    power = 2
    toughness = 3
    oracleText = "Flying\n" +
        "When this Vehicle enters, distribute two +1/+1 counters among one or two other target " +
        "Vehicles and/or creatures you control.\n" +
        "Crew 1"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target = TargetObject(
            count = 2,
            minCount = 1,
            filter = TargetFilter(
                GameObjectFilter.CreatureOrVehicle.youControl(),
                excludeSelf = true
            )
        )
        effect = Effects.DistributeCountersAmongTargets(totalCounters = 2)
        description = "When this Vehicle enters, distribute two +1/+1 counters among one or two other " +
            "target Vehicles and/or creatures you control."
    }

    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "197"
        artist = "Hardy Fowler"
        flavorText = "For Team Cloudspire, the track is merely a suggestion."
        imageUri = "https://cards.scryfall.io/normal/front/8/8/881f01a4-a923-4d56-bacb-984a389296fa.jpg?1782687806"
    }
}
