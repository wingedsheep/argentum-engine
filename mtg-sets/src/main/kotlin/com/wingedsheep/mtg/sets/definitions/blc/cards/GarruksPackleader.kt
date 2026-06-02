package com.wingedsheep.mtg.sets.definitions.blc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.MayEffect

/**
 * Garruk's Packleader
 * {4}{G}
 * Creature — Beast
 * 4/4
 *
 * Whenever another creature you control with power 3 or greater enters, you may draw a card.
 */
val GarruksPackleader = card("Garruk's Packleader") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "Whenever another creature you control with power 3 or greater enters, you may draw a card."

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().powerAtLeast(3),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.OTHER
        )
        effect = MayEffect(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "218"
        artist = "Nils Hamm"
        flavorText = "\"He has learned much in his long years. And unlike selfish humans, he's willing to share.\"\n—Garruk Wildspeaker"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/109dee37-5de5-40fa-9405-8645be6c5076.jpg?1721429269"

        ruling("2012-07-01", "The ability checks the power of each creature as it enters. It will take into account counters the permanent enters with and static abilities that affect its power. After the creature is on the battlefield, raising its power won't cause this ability to retroactively trigger.")
        ruling("2012-07-01", "The creature's power is checked only to see if the ability triggers. It doesn't matter what the creature's power is when the ability resolves.")
        ruling("2010-08-15", "If Garruk's Packleader and another creature with power 3 or greater enter under your control at the same time, the ability will trigger.")
    }
}
