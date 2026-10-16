package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CounterRemovalAmount
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.PreventDamageByRemovingCounter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bloatfly Swarm
 * {3}{B}
 * Creature — Insect Mutant
 * 0/0
 *
 * Magma Pummeler's prevention shape plus a rider: the rad counters are part of the replacement
 * itself ("…, then give each player a rad counter for each +1/+1 counter removed this way"), so they
 * ride `onRemoved` rather than a trigger — no stack, no priority window between the prevention and
 * the rad counters. The count is the counters actually removed, bounded by those present.
 */
val BloatflySwarm = card("Bloatfly Swarm") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Insect Mutant"
    power = 0
    toughness = 0
    oracleText = "Flying\n" +
        "This creature enters with five +1/+1 counters on it.\n" +
        "If damage would be dealt to this creature while it has a +1/+1 counter on it, prevent " +
        "that damage, remove that many +1/+1 counters from it, then give each player a rad counter " +
        "for each +1/+1 counter removed this way."

    keywords(Keyword.FLYING)

    replacementEffect(
        EntersWithCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 5, selfOnly = true)
    )

    replacementEffect(
        PreventDamageByRemovingCounter(
            removalAmount = CounterRemovalAmount.EqualToDamage,
            requiresCounter = true,
            onRemoved = Effects.ForEachPlayer(
                Player.Each,
                Effects.AddDynamicCounters(
                    CounterType.RAD,
                    DynamicAmounts.triggerCountersRemoved(),
                    EffectTarget.Controller
                )
            )
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "42"
        artist = "Igor Krstic"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d90b839-0808-435d-bcf3-aa7af414c44b.jpg?1783912410"
        ruling("2024-03-08", "If damage that can't be prevented is dealt to Bloatfly Swarm, you still remove that many +1/+1 counters from it, and you still give each player a rad counter for each +1/+1 counter removed this way.")
    }
}
