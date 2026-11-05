package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shadowed Caravel
 * {2}
 * Artifact — Vehicle
 * 2/2
 *
 * Whenever a creature you control explores, put a +1/+1 counter on this Vehicle.
 * Crew 2
 */
val ShadowedCaravel = card("Shadowed Caravel") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Vehicle"
    oracleText = "Whenever a creature you control explores, put a +1/+1 counter on this Vehicle.\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: " +
        "This Vehicle becomes an artifact creature until end of turn.)"
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).explores()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    keywordAbility(KeywordAbility.Numeric(Keyword.CREW, 2))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "246"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b28ab3ed-32cd-4168-9763-96884491aaa1.jpg"
    }
}
