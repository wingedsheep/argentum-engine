package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.SelectionMode

/**
 * Genesis Ultimatum
 * {G}{G}{U}{U}{U}{R}{R}
 * Sorcery
 *
 * Look at the top five cards of your library. Put any number of permanent cards from among them
 * onto the battlefield and the rest into your hand. Exile Genesis Ultimatum.
 *
 * "Exile Genesis Ultimatum" is the `selfExile()` flag: the resolved sorcery goes to exile instead
 * of the graveyard. A mid-resolution move of the spell itself would be overridden by the
 * resolver's post-resolution graveyard placement.
 */
val GenesisUltimatum = card("Genesis Ultimatum") {
    manaCost = "{G}{G}{U}{U}{U}{R}{R}"
    colorIdentity = "GRU"
    typeLine = "Sorcery"
    oracleText = "Look at the top five cards of your library. Put any number of permanent cards from " +
        "among them onto the battlefield and the rest into your hand. Exile Genesis Ultimatum."

    spell {
        effect = Patterns.Library.lookAtTopAndTakeMatching(
            count = DynamicAmounts.fixed(5),
            filter = GameObjectFilter.Permanent,
            prompt = "You may put any number of permanent cards from among them onto the battlefield",
            selection = SelectionMode.ChooseAnyNumber,
            keepDestination = CardDestination.ToZone(Zone.BATTLEFIELD),
            restDestination = CardDestination.ToZone(Zone.HAND),
            restOrder = CardOrder.Preserve,
        )
        selfExile()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "189"
        artist = "Jason Rainville"
        flavorText = "No human eyes have ever witnessed the birth of an apex monster."
        imageUri = "https://cards.scryfall.io/normal/front/5/5/556dfe7a-43e8-4801-ad79-89ab2148eca6.jpg"
    }
}
