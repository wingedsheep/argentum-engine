package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Glimpse the Sun God — Born of the Gods #13 (canonical / earliest real printing)
 * {X}{W} · Instant
 *
 * Tap X target creatures. Scry 1.
 *
 * Icy Blast's "exactly X targets" shape ([DynamicAmounts.xValue] as the target count) with
 * [Effects.TapEachTarget], followed by an unconditional [Effects.Scry].
 */
val GlimpseTheSunGod = card("Glimpse the Sun God") {
    manaCost = "{X}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Tap X target creatures. Scry 1. " +
        "(Look at the top card of your library. You may put that card on the bottom.)"

    spell {
        targets(TargetFilter.Creature, exactly = DynamicAmounts.xValue())
        effect = Effects.TapEachTarget() then Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Aleksi Briclot"
        flavorText = "Better to be beneath Heliod's notice than beneath his heel."
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f241e5f7-3168-4bd2-b586-185aad3ae9d9.jpg?1783939580"
    }
}
