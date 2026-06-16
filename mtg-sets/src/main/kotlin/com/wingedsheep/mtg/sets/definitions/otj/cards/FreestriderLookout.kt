package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Freestrider Lookout
 * {2}{G}
 * Creature — Human Rogue
 * 3/3
 * Reach
 * Whenever you commit a crime, look at the top five cards of your library. You may put a land
 * card from among them onto the battlefield tapped. Put the rest on the bottom of your library
 * in a random order. This ability triggers only once each turn. (Targeting opponents, anything
 * they control, and/or cards in their graveyards is a crime.)
 */
val FreestriderLookout = card("Freestrider Lookout") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Rogue"
    power = 3
    toughness = 3
    oracleText = "Reach\n" +
        "Whenever you commit a crime, look at the top five cards of your library. You may put a " +
        "land card from among them onto the battlefield tapped. Put the rest on the bottom of your " +
        "library in a random order. This ability triggers only once each turn. (Targeting opponents, " +
        "anything they control, and/or cards in their graveyards is a crime.)"

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.YouCommitCrime
        oncePerTurn = true
        description = "Whenever you commit a crime, look at the top five cards of your library. You may " +
            "put a land card from among them onto the battlefield tapped. Put the rest on the bottom of " +
            "your library in a random order. This ability triggers only once each turn."
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(5)),
                    storeAs = "looked"
                ),
                SelectFromCollectionEffect(
                    from = "looked",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    filter = GameObjectFilter.Land,
                    storeSelected = "kept",
                    storeRemainder = "rest",
                    prompt = "You may put a land card onto the battlefield tapped",
                    showAllCards = true
                ),
                MoveCollectionEffect(
                    from = "kept",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped)
                ),
                MoveCollectionEffect(
                    from = "rest",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
                    order = CardOrder.Random
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "163"
        artist = "Matt Zeilinger"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32370f05-52a2-405f-b2bb-1b8a9b0b69f8.jpg?1712355921"
    }
}
