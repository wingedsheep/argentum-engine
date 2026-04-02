package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Plumecreed Mentor
 * {1}{W}{U}
 * Creature — Bird Scout
 * 2/3
 *
 * Flying
 * Whenever this creature or another creature you control with flying enters,
 * put a +1/+1 counter on target creature you control without flying.
 */
val PlumecreedMentor = card("Plumecreed Mentor") {
    manaCost = "{1}{W}{U}"
    typeLine = "Creature — Bird Scout"
    oracleText = "Flying\nWhenever this creature or another creature you control with flying enters, " +
        "put a +1/+1 counter on target creature you control without flying."
    power = 2
    toughness = 3

    keywords(Keyword.FLYING)

    // Whenever this creature or another creature you control with flying enters
    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().withKeyword(Keyword.FLYING),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.ANY
        )
        val creatureWithoutFlying = target(
            "creature you control without flying",
            TargetCreature(
                filter = TargetFilter(
                    GameObjectFilter.Creature.youControl().withoutKeyword(Keyword.FLYING)
                )
            )
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creatureWithoutFlying)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "228"
        artist = "Henry Peters"
        flavorText = "\"Any fledgling can flap their wings. But a true windscout must soar!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1aa988f-547e-449a-9f1a-296c01d68d96.jpg?1721427166"
    }
}
