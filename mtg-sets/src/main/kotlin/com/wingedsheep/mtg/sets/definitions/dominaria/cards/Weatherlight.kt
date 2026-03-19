package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Weatherlight
 * {4}
 * Legendary Artifact — Vehicle
 * 4/5
 * Flying
 * Whenever Weatherlight deals combat damage to a player, look at the top five cards of your
 * library. You may reveal a historic card from among them and put it into your hand. Put the
 * rest on the bottom of your library in a random order.
 * Crew 3
 */
val Weatherlight = card("Weatherlight") {
    manaCost = "{4}"
    typeLine = "Legendary Artifact — Vehicle"
    power = 4
    toughness = 5
    oracleText = "Flying\nWhenever Weatherlight deals combat damage to a player, look at the top five cards of your library. You may reveal a historic card from among them and put it into your hand. Put the rest on the bottom of your library in a random order. (Artifacts, legendaries, and Sagas are historic.)\nCrew 3"

    keywords(Keyword.FLYING)

    keywordAbility(KeywordAbility.Crew(3))

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(5)),
                    storeAs = "looked"
                ),
                SelectFromCollectionEffect(
                    from = "looked",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    filter = GameObjectFilter.Historic,
                    storeSelected = "kept",
                    storeRemainder = "rest",
                    prompt = "You may reveal a historic card and put it into your hand",
                    showAllCards = true
                ),
                MoveCollectionEffect(
                    from = "kept",
                    destination = CardDestination.ToZone(Zone.HAND)
                ),
                MoveCollectionEffect(
                    from = "rest",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "237"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d4581fc0-551c-4ee5-bde0-65c2b8cdf1b7.jpg?1562743577"
        ruling("2018-04-27", "A card, spell, or permanent is historic if it has the legendary supertype, the artifact card type, or the Saga subtype. Having two of those qualities doesn't make an object more historic.")
    }
}
