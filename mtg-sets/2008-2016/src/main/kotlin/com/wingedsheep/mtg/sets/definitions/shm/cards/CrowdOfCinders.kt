package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Crowd of Cinders
 * {3}{B}
 * Creature — Elemental
 * * / *
 *
 * Fear (This creature can't be blocked except by artifact creatures and/or black creatures.)
 * Crowd of Cinders's power and toughness are each equal to the number of black permanents you control.
 *
 * - The P/T is a characteristic-defining ability (CR 604.3), so it is written with `dynamicStats`
 *   rather than a static `ModifyStats`: it applies in every zone and needs no printed base P/T.
 *   No `power`/`toughness` is set — the dynamic value is the whole characteristic.
 * - "black **permanents**", not creatures: the filter is `GameObjectFilter.Permanent.withColor`,
 *   which counts black lands, artifacts and enchantments too, and counts Crowd of Cinders itself
 *   while it is on the battlefield.
 * - `AggregateBattlefield` is the corpus's majority spelling for a one-battlefield tally (the same
 *   value as `Count(…, BATTLEFIELD, …)`); one printed form per model.
 */
val CrowdOfCinders = card("Crowd of Cinders") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Elemental"
    oracleText = "Fear (This creature can't be blocked except by artifact creatures and/or black creatures.)\n" +
        "Crowd of Cinders's power and toughness are each equal to the number of black permanents you control."

    keywords(Keyword.FEAR)

    dynamicStats(
        DynamicAmount.AggregateBattlefield(
            Player.You,
            GameObjectFilter.Permanent.withColor(Color.BLACK)
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Carl Frank"
        flavorText = "They envy the life-giving heat so much that they tear it from those who still possess it."
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0dd88305-d57b-4e77-aec9-f0a3ace42c37.jpg?1783942755"
    }
}
