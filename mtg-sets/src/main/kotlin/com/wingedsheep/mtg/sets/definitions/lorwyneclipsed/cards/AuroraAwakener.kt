package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement

/**
 * Aurora Awakener
 * {6}{G}
 * Creature — Giant Druid
 * 7/7
 *
 * Trample
 * Vivid — When this creature enters, reveal cards from the top of your library until you
 * reveal X permanent cards, where X is the number of colors among permanents you control.
 * Put any number of those permanent cards onto the battlefield, then put the rest of the
 * revealed cards on the bottom of your library in a random order.
 */
val AuroraAwakener = card("Aurora Awakener") {
    manaCost = "{6}{G}"
    typeLine = "Creature — Giant Druid"
    oracleText = "Trample\nVivid — When this creature enters, reveal cards from the top of your library " +
        "until you reveal X permanent cards, where X is the number of colors among permanents you control. " +
        "Put any number of those permanent cards onto the battlefield, then put the rest of the revealed " +
        "cards on the bottom of your library in a random order."
    power = 7
    toughness = 7

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CompositeEffect(listOf(
            GatherUntilMatchEffect(
                filter = GameObjectFilter.Permanent,
                storeMatch = "permanentsFound",
                storeRevealed = "allRevealed",
                count = DynamicAmounts.colorsAmongPermanents()
            ),
            RevealCollectionEffect(from = "allRevealed"),
            SelectFromCollectionEffect(
                from = "permanentsFound",
                selection = SelectionMode.ChooseAnyNumber,
                storeSelected = "toBattlefield",
                storeRemainder = "unchosenPermanents",
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Put on bottom of library"
            ),
            MoveCollectionEffect(
                from = "toBattlefield",
                destination = CardDestination.ToZone(Zone.BATTLEFIELD)
            ),
            // Everything revealed minus the cards that went to the battlefield goes to
            // the bottom of the library in a random order.
            FilterCollectionEffect(
                from = "allRevealed",
                filter = CollectionFilter.ExcludeOtherCollection("toBattlefield"),
                storeMatching = "toBottom"
            ),
            MoveCollectionEffect(
                from = "toBottom",
                destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                order = CardOrder.Random
            )
        ))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "165"
        artist = "Paolo Parente"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/913977c2-73f9-466b-bd01-827c1736e070.jpg?1767658343"
    }
}
