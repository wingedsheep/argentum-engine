package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.DynamicAmounts

/**
 * Marwyn, the Nurturer
 * {2}{G}
 * Legendary Creature — Elf Druid
 * 1/1
 * Whenever another Elf you control enters, put a +1/+1 counter on Marwyn.
 * {T}: Add an amount of {G} equal to Marwyn's power.
 */
val MarwynTheNurturer = card("Marwyn, the Nurturer") {
    manaCost = "{2}{G}"
    typeLine = "Legendary Creature — Elf Druid"
    power = 1
    toughness = 1
    oracleText = "Whenever another Elf you control enters, put a +1/+1 counter on Marwyn, the Nurturer.\n{T}: Add an amount of {G} equal to Marwyn's power."

    triggeredAbility {
        trigger = TriggerSpec(
            ZoneChangeEvent(
                filter = GameObjectFilter.Creature.withSubtype(Subtype("Elf")).youControl(),
                to = Zone.BATTLEFIELD
            ),
            TriggerBinding.OTHER
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN, DynamicAmounts.sourcePower())
        manaAbility = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "172"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/251613c0-2e72-4bd1-b3b5-69602b07b6f9.jpg?1562732806"
        ruling("2018-04-27", "Marwyn's activated ability is a mana ability. It doesn't use the stack and can't be responded to.")
    }
}
