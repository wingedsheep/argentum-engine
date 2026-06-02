package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec

/**
 * Mm'menon, Uthros Exile
 * {1}{U}{R}
 * Legendary Creature — Jellyfish Advisor
 * 1/3
 *
 * Flying
 * Whenever an artifact you control enters, put a +1/+1 counter on target creature.
 */
val MmmenonUthrosExile = card("Mm'menon, Uthros Exile") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Jellyfish Advisor"
    oracleText = "Flying\nWhenever an artifact you control enters, put a +1/+1 counter on target creature."
    power = 1
    toughness = 3

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Artifact
                    .youControl(),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.OTHER
        )

        val targetCreature = target("target creature", Targets.Creature)
        effect = Effects.AddCounters("+1/+1", 1, targetCreature)
        description = "Whenever an artifact you control enters, put a +1/+1 counter on target creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "221"
        artist = "Fajareka Setiawan"
        flavorText = "\"What crime did you commit that makes *me* your best option?\"\n—Tezzeret"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/5546c044-5826-48c3-9d28-866f3c3c5f2c.jpg?1752947461"
    }
}
