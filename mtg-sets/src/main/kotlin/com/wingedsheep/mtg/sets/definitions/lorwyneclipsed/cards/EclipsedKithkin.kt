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
 * Eclipsed Kithkin
 * {G/W}{G/W}
 * Creature — Kithkin Scout
 * 2/1
 *
 * When this creature enters, look at the top four cards of your library.
 * You may reveal a Kithkin, Forest, or Plains card from among them and
 * put it into your hand. Put the rest on the bottom of your library in
 * a random order.
 */
val EclipsedKithkin = card("Eclipsed Kithkin") {
    manaCost = "{G/W}{G/W}"
    typeLine = "Creature — Kithkin Scout"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, look at the top four cards of your library. You may reveal a Kithkin, Forest, or Plains card from among them and put it into your hand. Put the rest on the bottom of your library in a random order."

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
                            CardPredicate.HasSubtype(Subtype.KITHKIN),
                            CardPredicate.HasSubtype(Subtype.FOREST),
                            CardPredicate.HasSubtype(Subtype.PLAINS),
                        ),
                        matchAll = false
                    ),
                    storeSelected = "kept",
                    storeRemainder = "rest",
                    selectedLabel = "Put in hand",
                    remainderLabel = "Put on bottom",
                    showAllCards = true
                ),
                MoveCollectionEffect(
                    from = "kept",
                    destination = CardDestination.ToZone(Zone.HAND),
                    revealed = true,
                    revealToSelf = false
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
        collectorNumber = "220"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29e1cfa4-0ad8-4228-9f7e-cbab114d1d5f.jpg?1767952368"
    }
}
