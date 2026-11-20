package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reap What Is Sown
 * {1}{G}{W}
 * Instant
 * Put a +1/+1 counter on each of up to three target creatures.
 */
val ReapWhatIsSown = card("Reap What Is Sown") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Instant"
    oracleText = "Put a +1/+1 counter on each of up to three target creatures."

    spell {
        targets(TargetFilter.Creature, count = 3, optional = true)
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "154"
        artist = "Cynthia Sheppard"
        flavorText = "The strength of the people comes from the land. And the bounty of the land comes from Karametra."
        imageUri = "https://cards.scryfall.io/normal/front/1/2/120babed-841f-4bf1-ac2d-3ab097c0760b.jpg?1783939522"
        ruling("2014-02-01", "Reap What Is Sown can't target the same creature multiple times. It will put only one +1/+1 counter on each of its targets.")
    }
}
