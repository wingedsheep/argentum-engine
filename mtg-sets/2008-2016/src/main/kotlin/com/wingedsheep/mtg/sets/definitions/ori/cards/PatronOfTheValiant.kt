package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Patron of the Valiant
 * {3}{W}{W}
 * Creature — Angel
 * 4/4
 * Flying
 * When this creature enters, put a +1/+1 counter on each creature you control with a +1/+1 counter on it.
 */
val PatronOfTheValiant = card("Patron of the Valiant") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "When this creature enters, put a +1/+1 counter on each creature you control with a +1/+1 counter on it."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withCounter(CounterType.PLUS_ONE_PLUS_ONE).youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
        description = "When this creature enters, put a +1/+1 counter on each creature you control with a " +
            "+1/+1 counter on it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "Steve Argyle"
        flavorText = "\"Bant's most gallant knights wear my sigil, and for that I am humbled.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b5c0526-ac1f-40ca-a597-bf004f2e2377.jpg?1783938361"
    }
}
