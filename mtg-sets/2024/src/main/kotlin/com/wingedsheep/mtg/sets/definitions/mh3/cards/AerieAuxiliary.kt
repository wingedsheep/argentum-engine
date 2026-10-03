package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aerie Auxiliary — Modern Horizons 3 #18
 * {3}{W} · Creature — Bird Soldier · 3/3
 *
 * Flying
 * When this creature enters, support 2. (Put a +1/+1 counter on each of up to two other target
 * creatures.)
 *
 * Support has no engine keyword; it is lowered to its reminder text: up to two
 * *other* target creatures, any controller, each get one +1/+1 counter — the Angelic
 * Quartermaster shape.
 */
val AerieAuxiliary = card("Aerie Auxiliary") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird Soldier"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, support 2. (Put a +1/+1 counter on each of up to two other " +
        "target creatures.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.OtherCreature, count = 2, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
        )
        description = "When this creature enters, support 2 — put a +1/+1 counter on each of up " +
            "to two other target creatures."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "18"
        artist = "Donato Giancola"
        flavorText = "The hordechief's ground tactics were unsurpassed. If only he'd thought to " +
            "watch the skies."
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e4c134b-a416-467e-a158-def84c92c6af.jpg?1783911304"
    }
}
