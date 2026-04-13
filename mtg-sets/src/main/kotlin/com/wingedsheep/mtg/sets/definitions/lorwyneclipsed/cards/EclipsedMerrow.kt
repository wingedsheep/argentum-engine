package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Eclipsed Merrow
 * {W/U}{W/U}{W/U}
 * Creature — Merfolk Scout
 * 2/3
 *
 * When this creature enters, look at the top four cards of your library.
 * You may reveal a Merfolk, Plains, or Island card from among them and
 * put it into your hand. Put the rest on the bottom of your library in
 * a random order.
 */
val EclipsedMerrow = card("Eclipsed Merrow") {
    manaCost = "{W/U}{W/U}{W/U}"
    typeLine = "Creature — Merfolk Scout"
    power = 2
    toughness = 3
    oracleText = "When this creature enters, look at the top four cards of your library. You may reveal a Merfolk, Plains, or Island card from among them and put it into your hand. Put the rest on the bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(4)),
                    storeAs = "looked"
                ),
                SelectFromCollectionEffect(
                    from = "looked",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    filter = GameObjectFilter(
                        cardPredicates = listOf(
                            CardPredicate.HasSubtype(Subtype.MERFOLK),
                            CardPredicate.HasSubtype(Subtype.PLAINS),
                            CardPredicate.HasSubtype(Subtype.ISLAND),
                        ),
                        matchAll = false // OR: Merfolk OR Plains OR Island
                    ),
                    storeSelected = "kept",
                    storeRemainder = "rest",
                    selectedLabel = "Put in hand",
                    remainderLabel = "Put on bottom"
                ),
                MoveCollectionEffect(
                    from = "kept",
                    destination = CardDestination.ToZone(Zone.HAND),
                    revealed = true
                ),
                MoveCollectionEffect(
                    from = "rest",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "221"
        artist = "Chris Rahn"
        imageUri = "https://cards.scryfall.io/normal/front/2/3/2352750d-404d-4928-9bdb-1b0db599b70f.jpg?1767952376"
    }
}
