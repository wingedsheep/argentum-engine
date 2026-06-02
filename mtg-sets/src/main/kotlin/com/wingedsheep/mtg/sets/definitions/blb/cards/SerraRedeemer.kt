package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Serra Redeemer
 * {3}{W}{W}
 * Creature — Angel Soldier
 * 2/4
 *
 * Flying
 * Whenever another creature you control with power 2 or less enters,
 * put two +1/+1 counters on that creature.
 */
val SerraRedeemer = card("Serra Redeemer") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel Soldier"
    power = 2
    toughness = 4
    oracleText = "Flying\nWhenever another creature you control with power 2 or less enters, put two +1/+1 counters on that creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().powerAtMost(2),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.OTHER
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "387"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/230b9aef-bd9c-4332-ace4-b5b065bac6d8.jpg?1721428078"
        inBooster = false
    }
}
