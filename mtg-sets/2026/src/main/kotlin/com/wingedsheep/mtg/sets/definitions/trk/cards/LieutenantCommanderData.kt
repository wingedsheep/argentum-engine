package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lieutenant Commander Data
 * {2}{U}
 * Legendary Artifact Creature — Android Scientist
 * 2/2
 *
 * Whenever a player casts a noncreature spell, put a +1/+1 counter on Lieutenant Commander Data.
 * {1}, Remove three counters from among creatures you control: Draw a card.
 *
 * The removal cost takes counters of any kind, in any split across the creatures you control.
 */
val LieutenantCommanderData = card("Lieutenant Commander Data") {
    manaCost = "{2}{U}"
    typeLine = "Legendary Artifact Creature — Android Scientist"
    colorIdentity = "U"
    oracleText = "Whenever a player casts a noncreature spell, put a +1/+1 counter on Lieutenant Commander Data.\n" +
        "{1}, Remove three counters from among creatures you control: Draw a card."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts(GameObjectFilter.Noncreature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.RemoveCounters(count = 3, counterType = null, filter = Filters.Creature),
        )
        effect = Effects.DrawCards(1)
        description = "{1}, Remove three counters from among creatures you control: Draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "69"
        artist = "Irvin Rodriguez"
        flavorText = "\"Reasoning from the general to the specific. Is that not the very definition of deduction? Is that not the way Sherlock Holmes worked?\""
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6d5da51-6c74-4dd5-a532-cf4c2a7b601c.jpg?1792508182"
    }
}
