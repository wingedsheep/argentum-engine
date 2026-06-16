package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Converge (Oath of the Gatewatch; reused in Secrets of Strixhaven).
 *
 * **Converge is an ability word** — flavor only, no rules meaning of its own (CR 207.2c), so it
 * adds no keyword (mirrors how [opus] models its SOS sibling). Each Converge card scales some
 * effect by the number of distinct colors of mana spent to cast it; that count is
 * [DynamicAmount.DistinctColorsManaSpent] ([DynamicAmounts.colorsOfManaSpent]).
 *
 * The set's Converge cards come in three shapes:
 *  - **Enters with counters** (the "Archaic" cycle) — use [convergeEntersWithCounters].
 *  - **A spell whose effect scales** — read [DynamicAmounts.colorsOfManaSpent] directly in the
 *    spell's effect (e.g. a dynamic amount fed to draw/damage/token effects).
 *  - **Exile-by-color-count** — use the `manaValueAtMostColorsSpent(EntityReference.Source)`
 *    target/group predicate.
 *
 * In every case author the printed "Converge — …" reminder into the card's `oracleText`; the
 * mechanic carries no keyword tag.
 */

/**
 * Converge — "This creature enters with a [counterType] counter for each color of mana spent to
 * cast it." Composes an [EntersWithDynamicCounters] replacement effect fed by
 * [DynamicAmount.DistinctColorsManaSpent]. This is the dominant Converge shape (the SOS "Archaic"
 * cycle); defaults to +1/+1 counters.
 */
fun CardBuilder.convergeEntersWithCounters(
    counterType: CounterTypeFilter = CounterTypeFilter.PlusOnePlusOne,
) {
    replacementEffect(
        EntersWithDynamicCounters(
            counterType = counterType,
            count = DynamicAmount.DistinctColorsManaSpent,
        ),
    )
}
