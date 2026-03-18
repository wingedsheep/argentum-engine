package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.core.Zone

/**
 * Final Parting
 * {3}{B}{B}
 * Sorcery
 *
 * Search your library for two cards. Put one into your hand and the other
 * into your graveyard. Then shuffle.
 */
val FinalParting = card("Final Parting") {
    manaCost = "{3}{B}{B}"
    typeLine = "Sorcery"
    oracleText = "Search your library for two cards. Put one into your hand and the other into your graveyard. Then shuffle."

    spell {
        effect = CompositeEffect(listOf(
            // Gather all cards from library
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Any),
                storeAs = "searchable"
            ),
            // Select exactly 2 cards
            SelectFromCollectionEffect(
                from = "searchable",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(2)),
                storeSelected = "found",
                prompt = "Search your library for two cards"
            ),
            // From the 2 selected, choose 1 to put in hand (remainder goes to graveyard)
            SelectFromCollectionEffect(
                from = "found",
                selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                storeSelected = "toHand",
                storeRemainder = "toGraveyard",
                prompt = "Choose a card to put into your hand"
            ),
            // Move chosen card to hand
            MoveCollectionEffect(
                from = "toHand",
                destination = CardDestination.ToZone(Zone.HAND)
            ),
            // Move the other to graveyard
            MoveCollectionEffect(
                from = "toGraveyard",
                destination = CardDestination.ToZone(Zone.GRAVEYARD)
            ),
            // Shuffle library
            ShuffleLibraryEffect()
        ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "93"
        artist = "Eric Deschamps"
        flavorText = "\"Sleep now, brother. That is the one gift I can give you.\" — Liliana Vess"
        imageUri = "https://cards.scryfall.io/normal/front/d/e/de8803f6-9efa-4323-b8c5-29bdd5a48f9a.jpg?1562744124"
    }
}
