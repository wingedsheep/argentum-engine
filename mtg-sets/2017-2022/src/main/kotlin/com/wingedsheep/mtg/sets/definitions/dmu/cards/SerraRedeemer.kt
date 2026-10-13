package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
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
 *
 * Canonical printing is Dominaria United (its earliest real printing); Bloomburrow carries a
 * `Printing` row.
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
        trigger = Triggers.another(GameObjectFilter.Creature.youControl().powerAtMost(2)).enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "282"
        artist = "Joshua Raphael"
        flavorText = "\"Heroism is neither a strong arm nor a divine destiny. It is the choice to stand unbowed against impossible odds.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8b9cb5c-29f2-46ed-803e-c2170955217c.jpg?1783921243"

        ruling(
            "2022-09-09",
            "Serra Redeemer's second ability checks the power of a creature only at the moment it enters the battlefield. If it enters with counters, those counters are included. If that creature's power is less than 2 when it enters the battlefield but becomes greater than 2 after the ability triggers, you will still put two counters on it."
        )
    }
}
