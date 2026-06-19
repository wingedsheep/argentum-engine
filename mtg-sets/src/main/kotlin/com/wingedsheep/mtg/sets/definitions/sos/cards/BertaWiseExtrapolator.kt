package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.increment
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.CREATED_TOKENS
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Berta, Wise Extrapolator
 * {2}{G}{U}
 * Legendary Creature — Frog Druid
 * 1/4
 *
 * Increment (Whenever you cast a spell, if the amount of mana you spent is greater than
 * this creature's power or toughness, put a +1/+1 counter on this creature.)
 * Whenever one or more +1/+1 counters are put on Berta, add one mana of any color.
 * {X}, {T}: Create a 0/0 green and blue Fractal creature token and put X +1/+1 counters on it.
 */
val BertaWiseExtrapolator = card("Berta, Wise Extrapolator") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "UG"
    typeLine = "Legendary Creature — Frog Druid"
    power = 1
    toughness = 4
    oracleText = "Increment (Whenever you cast a spell, if the amount of mana you spent is greater than " +
        "this creature's power or toughness, put a +1/+1 counter on this creature.)\n" +
        "Whenever one or more +1/+1 counters are put on Berta, add one mana of any color.\n" +
        "{X}, {T}: Create a 0/0 green and blue Fractal creature token and put X +1/+1 counters on it."

    increment()

    // Whenever one or more +1/+1 counters are put on Berta, add one mana of any color.
    triggeredAbility {
        trigger = TriggerSpec(
            EventPattern.CountersPlacedEvent(
                counterType = Counters.PLUS_ONE_PLUS_ONE,
                filter = GameObjectFilter.Any,
            ),
            TriggerBinding.SELF,
        )
        effect = Effects.AddManaOfChoice()
        description = "Whenever one or more +1/+1 counters are put on Berta, add one mana of any color."
    }

    // {X}, {T}: Create a 0/0 Fractal and put X +1/+1 counters on it.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap)
        effect = Effects.Composite(
            Effects.CreateToken(
                power = 0,
                toughness = 0,
                colors = setOf(Color.GREEN, Color.BLUE),
                creatureTypes = setOf("Fractal"),
                imageUri = "https://cards.scryfall.io/normal/front/d/e/de564776-9d88-4533-8717-842eecdd0594.jpg?1775828279",
            ),
            Effects.AddDynamicCounters(
                counterType = Counters.PLUS_ONE_PLUS_ONE,
                amount = DynamicAmount.XValue,
                target = EffectTarget.PipelineTarget(CREATED_TOKENS, 0),
            ),
        )
        description = "{X}, {T}: Create a 0/0 green and blue Fractal creature token and put X +1/+1 counters on it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "175"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75f89c36-c81d-4580-9a5c-218fed0c5c9a.jpg?1775938201"
    }
}
