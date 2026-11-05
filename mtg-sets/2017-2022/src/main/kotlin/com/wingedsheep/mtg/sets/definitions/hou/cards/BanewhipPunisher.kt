package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Banewhip Punisher
 * {2}{B}
 * Creature — Human Warrior
 * 2/2
 *
 * When this creature enters, you may put a -1/-1 counter on target creature.
 * {B}, Sacrifice this creature: Destroy target creature that has a -1/-1 counter on it.
 */
val BanewhipPunisher = card("Banewhip Punisher") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warrior"
    oracleText = "When this creature enters, you may put a -1/-1 counter on target creature.\n" +
        "{B}, Sacrifice this creature: Destroy target creature that has a -1/-1 counter on it."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        val t = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 1, t)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.SacrificeSelf)
        val victim = target(
            TargetFilter(GameObjectFilter.Creature.withCounter(CounterType.MINUS_ONE_MINUS_ONE)),
        )
        effect = Effects.Destroy(victim)
        description = "{B}, Sacrifice this creature: Destroy target creature that has a -1/-1 counter on it."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Scott Murphy"
        flavorText = "\"If I die, it will not be alone.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/0/005a19e2-c334-4b87-bc8b-55f62fc9abd9.jpg?1783936042"
    }
}
