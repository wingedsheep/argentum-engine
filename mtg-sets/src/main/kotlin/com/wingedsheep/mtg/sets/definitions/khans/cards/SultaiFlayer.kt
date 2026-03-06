package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec

/**
 * Sultai Flayer
 * {3}{G}
 * Creature — Snake Shaman
 * 3/4
 * Whenever a creature you control with toughness 4 or greater dies, you gain 4 life.
 */
val SultaiFlayer = card("Sultai Flayer") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Snake Shaman"
    power = 3
    toughness = 4
    oracleText = "Whenever a creature you control with toughness 4 or greater dies, you gain 4 life."

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.youControl().toughnessAtLeast(4),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD
            ),
            binding = TriggerBinding.ANY
        )
        effect = Effects.GainLife(4)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "Izzy"
        flavorText = "\"You can have the body, necromancer. I just want the skin.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f3b2ef3-551d-4782-8dce-a923c0e2965e.jpg?1562789217"
    }
}
