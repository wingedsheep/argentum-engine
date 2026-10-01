package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters

/**
 * Font of Progress
 * {U}
 * Artifact
 *
 * This artifact enters with two oil counters on it.
 * {3}, {T}: Target player mills X cards, where X is the number of oil counters on this artifact.
 *
 * X is counted on resolution; if the artifact has left the battlefield by then, its last-known
 * oil-counter count is used (CR 608.2h).
 */
val FontOfProgress = card("Font of Progress") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "This artifact enters with two oil counters on it.\n" +
        "{3}, {T}: Target player mills X cards, where X is the number of oil counters on this artifact."

    replacementEffect(EntersWithCounters(counterType = CounterType.OIL, count = 2, selfOnly = true))

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(DynamicAmounts.countersOnSelf(CounterType.OIL), player)
        description = "{3}, {T}: Target player mills X cards, where X is the number of oil counters on this artifact."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "51"
        artist = "Aaron J. Riley"
        flavorText = "\"Drink deeply of the truth, and become *compleat*.\"\n—Tamiyo"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/272c9888-ef13-4d47-a4bc-f5239b357a1b.jpg?1783918065"
    }
}
