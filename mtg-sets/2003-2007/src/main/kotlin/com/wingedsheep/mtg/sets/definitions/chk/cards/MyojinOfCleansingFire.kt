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
 * Myojin of Cleansing Fire
 * {5}{W}{W}{W}
 * Legendary Creature — Spirit
 * 4/6
 *
 * Myojin of Cleansing Fire enters with a divinity counter on it if you cast it from your hand.
 * Myojin of Cleansing Fire has indestructible as long as it has a divinity counter on it.
 * Remove a divinity counter from Myojin of Cleansing Fire: Destroy all other creatures.
 */
val MyojinOfCleansingFire = card("Myojin of Cleansing Fire") {
    manaCost = "{5}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Spirit"
    power = 4
    toughness = 6
    oracleText = "Myojin of Cleansing Fire enters with a divinity counter on it if you cast it from your hand.\n" +
        "Myojin of Cleansing Fire has indestructible as long as it has a divinity counter on it.\n" +
        "Remove a divinity counter from Myojin of Cleansing Fire: Destroy all other creatures."

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
        effect = Effects.DestroyAll(GameObjectFilter.Creature.notSourceItself())
        description = "Remove a divinity counter from this creature: Destroy all other creatures."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0570ba0-2877-46f6-acea-6913f8915d6d.jpg?1783944334"
        ruling(
            "2013-07-01",
            "In a Commander game where this card is your commander, casting it from the command zone " +
                "does not count as casting it from your hand.",
        )
    }
}
