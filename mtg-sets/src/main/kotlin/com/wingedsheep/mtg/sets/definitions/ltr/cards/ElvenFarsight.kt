package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Elven Farsight
 * {G}
 * Sorcery
 * Scry 3, then you may reveal the top card of your library. If it's a creature card, draw a card.
 */
val ElvenFarsight = card("Elven Farsight") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Scry 3, then you may reveal the top card of your library. If it's a creature card, draw a card."

    spell {
        // Scry 3, then reveal the top card; if it's a creature card, put it into your hand (draw it).
        effect = LibraryPatterns.scry(3) then
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
                storeAs = "revealed",
                revealed = true
            ) then
            SelectFromCollectionEffect(
                from = "revealed",
                selection = SelectionMode.All,
                filter = GameObjectFilter.Creature,
                storeSelected = "creature",
                storeRemainder = "noncreature"
            ) then
            MoveCollectionEffect(
                from = "creature",
                destination = CardDestination.ToZone(Zone.HAND)
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "161"
        artist = "Irina Nordsol"
        flavorText = "\"Few can foresee whither their road will lead them, till they come to its end.\"\n—Legolas"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73d135b2-d2b8-499c-84d9-824370c19ccc.jpg?1686969313"
    }
}
