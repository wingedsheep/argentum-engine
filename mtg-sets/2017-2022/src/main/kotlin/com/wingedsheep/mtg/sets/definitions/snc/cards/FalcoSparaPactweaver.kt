package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary

/**
 * Falco Spara, Pactweaver
 * {1}{G}{W}{U}
 * Legendary Creature — Bird Demon
 * 3/3
 * Flying, trample
 * Falco Spara enters with a shield counter on it.
 * You may look at the top card of your library any time.
 * You may cast spells from the top of your library by removing a counter from a creature you
 * control in addition to paying their other costs.
 *
 * The cast-from-top permission is [CastSpellTypesFromTopOfLibrary] carrying an additional cost:
 * removing one counter of any kind from a creature you control, paid alongside the spell's mana
 * cost (Falco's own shield counter qualifies).
 */
val FalcoSparaPactweaver = card("Falco Spara, Pactweaver") {
    manaCost = "{1}{G}{W}{U}"
    colorIdentity = "GWU"
    typeLine = "Legendary Creature — Bird Demon"
    power = 3
    toughness = 3
    oracleText = "Flying, trample\n" +
        "Falco Spara enters with a shield counter on it.\n" +
        "You may look at the top card of your library any time.\n" +
        "You may cast spells from the top of your library by removing a counter from a creature you " +
        "control in addition to paying their other costs."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.SHIELD,
            count = 1,
            selfOnly = true,
        )
    )

    staticAbility {
        ability = LookAtTopOfLibrary
    }

    staticAbility {
        ability = CastSpellTypesFromTopOfLibrary(
            filter = GameObjectFilter.Nonland,
            additionalCost = Costs.additional.RemoveCounters(
                counterType = null,
                count = 1,
                filter = GameObjectFilter.Creature,
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "186"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae25db8c-3d10-4196-b002-9d2aabd5f4de.jpg?1783923087"
    }
}
