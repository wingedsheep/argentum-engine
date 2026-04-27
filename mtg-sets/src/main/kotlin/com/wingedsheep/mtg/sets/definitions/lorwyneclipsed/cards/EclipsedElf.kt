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
 * Eclipsed Elf
 * {B/G}{B/G}{B/G}
 * Creature — Elf Scout
 * 3/2
 *
 * When this creature enters, look at the top four cards of your library.
 * You may reveal an Elf, Swamp, or Forest card from among them and put
 * it into your hand. Put the rest on the bottom of your library in a
 * random order.
 */
val EclipsedElf = card("Eclipsed Elf") {
    manaCost = "{B/G}{B/G}{B/G}"
    typeLine = "Creature — Elf Scout"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, look at the top four cards of your library. You may reveal an Elf, Swamp, or Forest card from among them and put it into your hand. Put the rest on the bottom of your library in a random order."

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
                            CardPredicate.HasSubtype(Subtype.ELF),
                            CardPredicate.HasSubtype(Subtype.SWAMP),
                            CardPredicate.HasSubtype(Subtype.FOREST),
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
        collectorNumber = "218"
        artist = "Jason A. Engle"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c8579f3-6125-4f22-b1c2-b7a0cfc50eed.jpg?1767952353"
    }
}
