package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Death's Presence — Return to Ravnica #121 (canonical printing)
 * {5}{G} · Enchantment
 *
 * Whenever a creature you control dies, put X +1/+1 counters on target creature you control,
 * where X is the power of the creature that died.
 *
 * X reads the dying creature's last-known power via `DynamicAmounts.triggeringPower()`, the same
 * shape Vincent Valentine uses for "equal to that creature's power".
 */
val DeathsPresence = card("Death's Presence") {
    manaCost = "{5}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control dies, put X +1/+1 counters on target creature you " +
        "control, where X is the power of the creature that died."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dies()
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddDynamicCounters(
            CounterType.PLUS_ONE_PLUS_ONE,
            DynamicAmounts.triggeringPower(),
            creature
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "121"
        artist = "Ryan Barger"
        flavorText = "\"We traffic in flesh, not souls. Still, it's a shame to let anything go to waste.\"\n" +
            "—Cevraya, Golgari shaman"
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa82c57d-4bb9-407c-b973-7abc793b6f47.jpg?1783940349"
    }
}
