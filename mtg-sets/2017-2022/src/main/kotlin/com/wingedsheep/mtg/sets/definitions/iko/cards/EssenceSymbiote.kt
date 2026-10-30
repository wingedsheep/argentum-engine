package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Essence Symbiote
 * {1}{G}
 * Creature — Beast
 * 2/2
 * Whenever a creature you control mutates, put a +1/+1 counter on that creature and you gain 2 life.
 */
val EssenceSymbiote = card("Essence Symbiote") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 2
    toughness = 2
    oracleText = "Whenever a creature you control mutates, put a +1/+1 counter on that creature and you gain 2 life."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).mutates()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity) then
            Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "149"
        artist = "Jason Felix"
        flavorText = "The Ozolith's appearance spawned a new era of wild mutation on Ikoria. Some creatures hunted different prey, lived in new habitats, and thrived on mutation itself."
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d09ddf0-91f0-4e76-809f-c39ca7418ed5.jpg?1783931039"
    }
}
