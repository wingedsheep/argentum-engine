package com.wingedsheep.mtg.sets.definitions.grn.cards

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
 * Venerated Loxodon
 * {4}{W}
 * Creature — Elephant Cleric
 * 4/4
 * Convoke
 * When this creature enters, put a +1/+1 counter on each creature that convoked it.
 *
 * "Each creature that convoked it" (CR 702.51c) is a group over
 * `GameObjectFilter.Creature.thatConvokedSource()` — the creatures tapped for its convoke that are
 * still the same objects on the battlefield (same shape as Zephyr Singer).
 */
val VeneratedLoxodon = card("Venerated Loxodon") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Elephant Cleric"
    power = 4
    toughness = 4
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while " +
        "casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "When this creature enters, put a +1/+1 counter on each creature that convoked it."

    keywords(Keyword.CONVOKE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.thatConvokedSource()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "Zack Stella"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93599fba-7c53-4621-85b5-6434be077bf6.jpg?1783934195"
        ruling(
            "2018-10-05",
            "You can't tap more creatures to convoke Venerated Loxodon than it takes to pay for its total cost. This means that normally no more than five creatures can convoke it."
        )
    }
}
