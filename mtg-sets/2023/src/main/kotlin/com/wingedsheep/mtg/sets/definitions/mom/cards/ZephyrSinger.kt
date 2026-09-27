package com.wingedsheep.mtg.sets.definitions.mom.cards

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
 * Zephyr Singer
 * {2}{U}{U}
 * Creature — Siren Pirate
 * 3/4
 * Convoke
 * Flying, vigilance
 * When this creature enters, put a flying counter on each creature that convoked it.
 *
 * "Each creature that convoked it" (CR 702.51c) is a group over
 * `GameObjectFilter.Creature.thatConvokedSource()` — the creatures tapped for its convoke that are
 * still the same objects (one that left and came back is a new object, CR 400.7).
 */
val ZephyrSinger = card("Zephyr Singer") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Siren Pirate"
    power = 3
    toughness = 4
    oracleText = "Convoke (Your creatures can help cast this spell. Each creature you tap while " +
        "casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Flying, vigilance\n" +
        "When this creature enters, put a flying counter on each creature that convoked it."

    keywords(Keyword.CONVOKE, Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.thatConvokedSource()),
            Effects.AddCounters(CounterType.FLYING, 1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "86"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/9678966b-65f1-405c-b8db-5f2d4f434933.jpg?1783917021"
        ruling(
            "2023-04-14",
            "You can't tap more creatures to convoke Zephyr Singer than is necessary to pay for the spell. In most cases, this means four creatures. However, if there are any additional costs to cast Zephyr Singer, you may use convoke to pay those additional costs as well."
        )
    }
}
