package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleCounterPlacement
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.MultiplyTokenCreation
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.events.RecipientFilter

/**
 * Doubling Season
 * {4}{G}
 * Enchantment
 * If an effect would create one or more tokens under your control, it creates twice that many
 * of those tokens instead.
 * If an effect would put one or more counters on a permanent you control, it puts twice that
 * many of those counters on that permanent instead.
 *
 * Both clauses are static replacement effects. Token doubling uses the default
 * `TokenCreationEvent(controller = You)`. Counter doubling gates purely on the recipient
 * ("a permanent you control"), independent of who places the counters — hence
 * `placedByYou = false` with `RecipientFilter.PermanentYouControl` and any counter type.
 */
val DoublingSeason = card("Doubling Season") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText =
        "If an effect would create one or more tokens under your control, it creates twice that many of those tokens instead.\n" +
        "If an effect would put one or more counters on a permanent you control, it puts twice that many of those counters on that permanent instead."

    replacementEffect(MultiplyTokenCreation())

    replacementEffect(
        DoubleCounterPlacement(
            placedByYou = false,
            appliesTo = EventPattern.CounterPlacementEvent(
                counterType = CounterTypeFilter.Any,
                recipient = RecipientFilter.PermanentYouControl,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "158"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7e71299-98f6-494e-b187-8d22ce5f50af.jpg?1783943641"
    }
}
