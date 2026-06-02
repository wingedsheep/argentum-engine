package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReplaceDrawWithEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

// Oracle errata: Original text used "remove from the game" (now "exile").
// Rulings:
// - If you have fewer than 7 cards in your library, exile them all.
// - You can choose to take the top card from the pile even if the pile is empty.
// - You can't look at the face-down cards.
val ParallelThoughts = card("Parallel Thoughts") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, search your library for seven cards, exile them " +
        "in a face-down pile, and shuffle that pile. Then shuffle your library.\n" +
        "If you would draw a card, you may instead put the top card of the pile you exiled into your hand."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Any),
                storeAs = "searchable"
            ),
            SelectFromCollectionEffect(
                from = "searchable",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(7)),
                storeSelected = "found"
            ),
            MoveCollectionEffect(
                from = "found",
                destination = CardDestination.ToZone(Zone.EXILE),
                order = CardOrder.Random,
                linkToSource = true,
                faceDown = true
            ),
            ShuffleLibraryEffect()
        ))
    }

    replacementEffect(
        ReplaceDrawWithEffect(
            replacementEffect = Effects.TakeFromLinkedExile(),
            optional = true
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "44"
        artist = "Ben Thompson"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d913c541-a8fb-4383-bbab-988be3e0f5d5.jpg?1562535276"
    }
}
