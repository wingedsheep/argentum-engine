package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ninth Bridge Patrol — Kaladesh #22
 * {1}{W} · Creature — Dwarf Soldier · 1/1 · Common
 *
 * Whenever another creature you control leaves the battlefield, put a +1/+1 counter on this creature.
 *
 * "Leaves the battlefield", not "dies": the trigger is a destination-less zone change off the
 * battlefield, so bounce and exile count as much as death (second ruling).
 */
val NinthBridgePatrol = card("Ninth Bridge Patrol") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Soldier"
    power = 1
    toughness = 1
    oracleText = "Whenever another creature you control leaves the battlefield, put a +1/+1 counter " +
        "on this creature."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).leaves()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever another creature you control leaves the battlefield, put a +1/+1 " +
            "counter on this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Ryan Alexander Lee"
        flavorText = "The Consulate keeps watch at each of the eleven bridges to ensure that the market " +
            "remains a haven for commerce, not for con artists."
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10a22cf8-4441-48ab-8adc-ae071cbc5999.jpg?1783937230"
        ruling(
            "2020-11-10",
            "If Ninth Bridge Patrol is dealt lethal damage at the same time as another creature you " +
                "control, it won't receive a counter from its ability in time to save it."
        )
        ruling(
            "2020-11-10",
            "Ninth Bridge Patrol's ability doesn't care where the creature went or whether it's a " +
                "creature in its new zone. It may have died, been exiled, returned to your hand, and so " +
                "on. It won't trigger if an object remains on the battlefield but ceases to be a " +
                "creature. It won't trigger if a creature phases out."
        )
    }
}
