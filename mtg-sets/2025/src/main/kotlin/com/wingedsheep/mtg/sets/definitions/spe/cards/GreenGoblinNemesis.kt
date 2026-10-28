package com.wingedsheep.mtg.sets.definitions.spe.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Green Goblin, Nemesis
 * {2}{B}{R}
 * Legendary Creature — Goblin Human Villain
 * 3/3
 * Flying
 * Whenever you discard a nonland card, put a +1/+1 counter on target Goblin you control.
 * Whenever you discard a land card, create a tapped Treasure token.
 *
 * Two typed discard triggers (Surly Badgersaur's shape), each firing once per matching discarded
 * card. "Target Goblin" is any Goblin permanent you control, not only a creature.
 */
val GreenGoblinNemesis = card("Green Goblin, Nemesis") {
    manaCost = "{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Goblin Human Villain"
    oracleText = "Flying\n" +
        "Whenever you discard a nonland card, put a +1/+1 counter on target Goblin you control.\n" +
        "Whenever you discard a land card, create a tapped Treasure token."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.discards(GameObjectFilter.Nonland)
        val goblin = target(TargetFilter(GameObjectFilter.Permanent.youControl().withSubtype("Goblin")))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, goblin)
        description = "Whenever you discard a nonland card, put a +1/+1 counter on target Goblin you control."
    }

    triggeredAbility {
        trigger = Triggers.you.discards(GameObjectFilter.Land)
        effect = Effects.CreateTreasure(1, tapped = true)
        description = "Whenever you discard a land card, create a tapped Treasure token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "Dan Dos Santos"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2860277-5b9e-444a-81f9-58e23355a722.jpg?1783905367"
    }
}
