package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bloodbriar (Eldritch Moon #151)
 * {2}{G}
 * Creature — Plant Elemental
 * 2/3
 *
 * Whenever you sacrifice another permanent, put a +1/+1 counter on this creature.
 */
val Bloodbriar = card("Bloodbriar") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Plant Elemental"
    power = 2
    toughness = 3
    oracleText = "Whenever you sacrifice another permanent, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.you.sacrificesAnother(GameObjectFilter.Permanent)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever you sacrifice another permanent, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "151"
        artist = "Tomasz Jedruszek"
        flavorText = "\"There is a sourness here that I don't understand. When the land reaches out to me, " +
            "I hesitate to reach back.\"\n—Nissa Revane"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9f1d82e-876f-48a6-8153-f02a44cf90a8.jpg"
    }
}
