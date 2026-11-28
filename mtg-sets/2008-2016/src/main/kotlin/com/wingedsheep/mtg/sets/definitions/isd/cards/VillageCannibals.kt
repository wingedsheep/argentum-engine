package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Village Cannibals
 * {2}{B}
 * Creature — Human
 * 2/2
 * Whenever another Human creature dies, put a +1/+1 counter on this creature.
 */
val VillageCannibals = card("Village Cannibals") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human"
    oracleText = "Whenever another Human creature dies, put a +1/+1 counter on this creature."
    power = 2
    toughness = 2
    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.withSubtype(Subtype.HUMAN)).dies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Bud Cook"
        flavorText = "Some have endured the horrors of Innistrad by becoming the worst monsters of all."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5400460-da9d-437b-bb81-cf382beb371e.jpg"
    }
}
