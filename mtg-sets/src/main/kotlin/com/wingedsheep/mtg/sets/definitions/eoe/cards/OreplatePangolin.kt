package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oreplate Pangolin
 * {1}{R}
 * Artifact Creature — Robot Pangolin
 * Whenever another artifact you control enters, you may pay {1}. If you do, put a +1/+1 counter on this creature.
 * 2/2
 */
val OreplatePangolin = card("Oreplate Pangolin") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Robot Pangolin"
    power = 2
    toughness = 2
    oracleText = "Whenever another artifact you control enters, you may pay {1}. If you do, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Artifact
                    .youControl(),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.OTHER
        )
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{1}"),
            effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        )
        description = "Whenever another artifact you control enters, you may pay {1}. If you do, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "150"
        artist = "Dmitry Burmak"
        flavorText = "It wasn't the most efficient design, but the miners refused to part with \"Ol' Pangy.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90209957-95db-4b59-979a-316d14ef876c.jpg?1752947160"
    }
}
