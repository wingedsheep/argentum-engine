package com.wingedsheep.mtg.sets.definitions.fdn.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.CountersPlacedEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Exemplar of Light
 * {2}{W}{W}
 * Creature — Angel
 * 3/3
 *
 * Flying
 * Whenever you gain life, put a +1/+1 counter on this creature.
 * Whenever you put one or more +1/+1 counters on this creature, draw a card. This ability triggers only once each turn.
 */
val ExemplarOfLight = card("Exemplar of Light") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 3
    toughness = 3
    oracleText = "Flying\nWhenever you gain life, put a +1/+1 counter on this creature.\nWhenever you put one or more +1/+1 counters on this creature, draw a card. This ability triggers only once each turn."

    keywords(Keyword.FLYING)

    // Whenever you gain life, put a +1/+1 counter on this creature
    triggeredAbility {
        trigger = Triggers.YouGainLife
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    // Whenever you put one or more +1/+1 counters on this creature, draw a card. This ability triggers only once each turn.
    triggeredAbility {
        trigger = TriggerSpec(
            event = CountersPlacedEvent(
                counterType = "+1/+1",
                filter = GameObjectFilter.Any
            ),
            binding = TriggerBinding.SELF
        )
        effect = Effects.DrawCards(1)
        oncePerTurn = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "733"
        artist = "Ekaterina Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7393efb-da40-4b55-a6ff-ce774f0815f9.jpg?1775599652"
    }
}
