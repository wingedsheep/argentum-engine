package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.SetMaximumHandSize
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Midnight Oil — Kaladesh #92
 * {2}{B}{B} · Enchantment
 *
 * This enchantment enters with seven hour counters on it.
 * At the beginning of your draw step, draw an additional card and remove two hour counters from
 * this enchantment.
 * Your maximum hand size is equal to the number of hour counters on this enchantment.
 * Whenever you discard a card, you lose 1 life.
 *
 * The usual discard is the cleanup step's discard to maximum hand size, so the life-loss triggers
 * go on the stack in that cleanup step and its controller gets priority there (CR 514.3a).
 */
val MidnightOil = card("Midnight Oil") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "This enchantment enters with seven hour counters on it.\n" +
        "At the beginning of your draw step, draw an additional card and remove two hour counters " +
        "from this enchantment.\n" +
        "Your maximum hand size is equal to the number of hour counters on this enchantment.\n" +
        "Whenever you discard a card, you lose 1 life."

    replacementEffect(EntersWithCounters(counterType = CounterType.HOUR, count = 7, selfOnly = true))

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.DRAW)
        effect = Effects.DrawCards(1) then Effects.RemoveCounters(CounterType.HOUR, 2, EffectTarget.Self)
    }

    staticAbility {
        ability = SetMaximumHandSize(Player.You, DynamicAmounts.countersOnSelf(CounterType.HOUR))
    }

    triggeredAbility {
        trigger = Triggers.you.discards()
        effect = Effects.LoseLife(1, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14875840-044d-482f-845e-79240cad66f3.jpg?1783937203"
    }
}
