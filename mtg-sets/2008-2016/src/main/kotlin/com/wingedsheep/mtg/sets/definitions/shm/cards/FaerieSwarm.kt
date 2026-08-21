package com.wingedsheep.mtg.sets.definitions.shm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Faerie Swarm
 * {3}{U}
 * Creature — Faerie
 * * / *
 *
 * Flying
 * Faerie Swarm's power and toughness are each equal to the number of blue permanents you control.
 *
 * - The P/T is a characteristic-defining ability (CR 604.3): `dynamicStats`, no printed base P/T.
 * - "blue **permanents**", not creatures — the filter is `GameObjectFilter.Permanent.withColor`,
 *   so blue lands, artifacts and enchantments count, as does Faerie Swarm itself on the battlefield.
 */
val FaerieSwarm = card("Faerie Swarm") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Faerie"
    oracleText = "Flying\n" +
        "Faerie Swarm's power and toughness are each equal to the number of blue permanents you control."

    keywords(Keyword.FLYING)

    dynamicStats(
        DynamicAmount.AggregateBattlefield(
            Player.You,
            GameObjectFilter.Permanent.withColor(Color.BLUE)
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "37"
        artist = "Zoltan Boros & Gabor Szikszai"
        flavorText = "Untouched by the Aurora, Oona's faeries greeted the night like any other day."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90598bb2-d96c-4f74-864b-1947b7d39e58.jpg?1783942761"
    }
}
