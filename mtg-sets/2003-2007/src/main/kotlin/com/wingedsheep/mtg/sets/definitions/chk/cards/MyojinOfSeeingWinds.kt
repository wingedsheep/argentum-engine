package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Myojin of Seeing Winds
 * {7}{U}{U}{U}
 * Legendary Creature — Spirit
 * 3/3
 *
 * Myojin of Seeing Winds enters with a divinity counter on it if you cast it from your hand.
 * Myojin of Seeing Winds has indestructible as long as it has a divinity counter on it.
 * Remove a divinity counter from Myojin of Seeing Winds: Draw a card for each permanent you control.
 */
val MyojinOfSeeingWinds = card("Myojin of Seeing Winds") {
    manaCost = "{7}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Spirit"
    power = 3
    toughness = 3
    oracleText = "Myojin of Seeing Winds enters with a divinity counter on it if you cast it from your hand.\n" +
        "Myojin of Seeing Winds has indestructible as long as it has a divinity counter on it.\n" +
        "Remove a divinity counter from Myojin of Seeing Winds: Draw a card for each permanent you control."

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

    // The count is taken on resolution and includes the Myojin itself if it is still there.
    activatedAbility {
        cost = Costs.RemoveCounterFromSelf(CounterType.DIVINITY)
        effect = Effects.DrawCards(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent).count()
        )
        description = "Remove a divinity counter from this creature: Draw a card for each permanent you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f5f8d3a-95e7-4dd9-8510-43517eb02693.jpg?1783944324"
        ruling(
            "2013-07-01",
            "In a Commander game where this card is your commander, casting it from the Command zone " +
                "does not count as casting it from your hand.",
        )
    }
}
