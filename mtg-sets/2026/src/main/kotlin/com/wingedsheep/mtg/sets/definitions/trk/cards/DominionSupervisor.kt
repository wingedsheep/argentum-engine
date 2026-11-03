package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dominion Supervisor
 * {2}{G}
 * Creature — Vorta Doctor
 * 3/1
 *
 * When this creature enters, put a +1/+1 counter on target creature.
 * Creatures you control with counters on them have trample.
 *
 * Badgermole's trample grant, widened from +1/+1 counters to counters of any kind.
 */
val DominionSupervisor = card("Dominion Supervisor") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Vorta Doctor"
    power = 3
    toughness = 1
    oracleText = "When this creature enters, put a +1/+1 counter on target creature.\n" +
        "Creatures you control with counters on them have trample."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    staticAbility {
        ability = GrantKeyword(
            Keyword.TRAMPLE,
            GroupFilter(GameObjectFilter.Creature.youControl().withAnyCounter()),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "195"
        artist = "Kevin Glint"
        flavorText = "The loyal Vorta saw to the distribution of ketracel-white, the Dominion's powerful " +
            "combination of narcotics and life-giving enzymes."
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5fa76320-24c0-4294-8ae4-fb39e171fbe6.jpg?1785981577"
    }
}
