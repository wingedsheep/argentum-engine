package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Monoskelion — Modern Horizons 2 #229
 * {2} · Artifact Creature — Construct · 1 / 1
 *
 * This creature enters with a +1/+1 counter on it.
 * {1}, Remove a +1/+1 counter from this creature: It deals 1 damage to any target.
 *
 * Triskelion's two halves at one-third scale, plus a mana pip on the activation. The printed box is
 * 1/1 and the counter is a separate [EntersWithCounters] replacement (`selfOnly`), so a Monoskelion
 * on the battlefield is a 2/2 that shrinks back to 1/1 the moment it shoots — the counter is both
 * the ammunition and the stat line, which is the whole card.
 *
 * The two counter vocabularies are not interchangeable: replacement effects name a counter with
 * [CounterTypeFilter.PlusOnePlusOne], while costs and effects name one with the [Counters] string
 * constant. Removing the counter is a *cost*, so it is paid on activation and the damage is on the
 * stack independently — the ability still resolves if the Monoskelion dies in response.
 */
val Monoskelion = card("Monoskelion") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 1
    toughness = 1
    oracleText = "This creature enters with a +1/+1 counter on it.\n" +
        "{1}, Remove a +1/+1 counter from this creature: It deals 1 damage to any target."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterTypeFilter.PlusOnePlusOne,
            count = 1,
            selfOnly = true
        )
    )

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.RemoveCounterFromSelf(Counters.PLUS_ONE_PLUS_ONE, 1)
        )
        val t = target("target", AnyTarget())
        effect = Effects.DealDamage(1, t)
        description = "{1}, Remove a +1/+1 counter from this creature: It deals 1 damage to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "229"
        artist = "Jason A. Engle"
        flavorText = "An unfinished body with unfinished business."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/4736aae2-5136-4f8f-9283-baf6b542a6a8.jpg?1783926802"
    }
}
