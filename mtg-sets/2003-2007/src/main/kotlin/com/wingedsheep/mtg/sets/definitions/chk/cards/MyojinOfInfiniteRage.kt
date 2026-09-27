package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Myojin of Infinite Rage
 * {7}{R}{R}{R}
 * Legendary Creature — Spirit
 * 7/4
 *
 * Myojin of Infinite Rage enters with a divinity counter on it if you cast it from your hand.
 * Myojin of Infinite Rage has indestructible as long as it has a divinity counter on it.
 * Remove a divinity counter from Myojin of Infinite Rage: Destroy all lands.
 */
val MyojinOfInfiniteRage = card("Myojin of Infinite Rage") {
    manaCost = "{7}{R}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Spirit"
    power = 7
    toughness = 4
    oracleText = "Myojin of Infinite Rage enters with a divinity counter on it if you cast it from your hand.\n" +
        "Myojin of Infinite Rage has indestructible as long as it has a divinity counter on it.\n" +
        "Remove a divinity counter from Myojin of Infinite Rage: Destroy all lands."

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.DIVINITY,
            count = 1,
            selfOnly = true,
            condition = Conditions.WasCastFromHand,
        )
    )

    // Filters.Self: GrantKeyword's default filter is attached-scope (Aura/Equipment shape).
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.INDESTRUCTIBLE, Filters.Self),
            condition = Conditions.SourceHasCounter(CounterType.DIVINITY),
        )
    }

    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.DIVINITY)
        effect = Effects.DestroyAll(GameObjectFilter.Land)
        description = "Remove a divinity counter from this creature: Destroy all lands."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "181"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df630ae9-aa17-46e9-95e7-4980f4a76ade.jpg?1783944297"
        ruling(
            "2024-11-08",
            "In a Commander game where this card is your commander, casting it from the command zone " +
                "does not count as casting it from your hand.",
        )
    }
}
