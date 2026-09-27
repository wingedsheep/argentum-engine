package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Myojin of Life's Web
 * {6}{G}{G}{G}
 * Legendary Creature — Spirit
 * 8/8
 *
 * Myojin of Life's Web enters with a divinity counter on it if you cast it from your hand.
 * Myojin of Life's Web has indestructible as long as it has a divinity counter on it.
 * Remove a divinity counter from Myojin of Life's Web: Put any number of creature cards from
 * your hand onto the battlefield.
 */
val MyojinOfLifesWeb = card("Myojin of Life's Web") {
    manaCost = "{6}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Spirit"
    power = 8
    toughness = 8
    oracleText = "Myojin of Life's Web enters with a divinity counter on it if you cast it from your hand.\n" +
        "Myojin of Life's Web has indestructible as long as it has a divinity counter on it.\n" +
        "Remove a divinity counter from Myojin of Life's Web: Put any number of creature cards from your hand onto the battlefield."

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
        effect = Patterns.Hand.putFromHand(
            filter = GameObjectFilter.Creature,
            anyNumber = true,
            prompt = "Put any number of creature cards from your hand onto the battlefield",
        )
        description = "Remove a divinity counter from this creature: Put any number of creature cards " +
            "from your hand onto the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "229"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/efb926ad-e762-4883-8841-73e034d8e21e.jpg?1783944286"
        ruling(
            "2013-07-01",
            "In a Commander game where this card is your commander, casting it from the Command zone " +
                "does not count as casting it from your hand.",
        )
    }
}
