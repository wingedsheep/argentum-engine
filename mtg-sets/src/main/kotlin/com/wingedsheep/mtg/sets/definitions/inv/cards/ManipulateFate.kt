package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Manipulate Fate
 * {1}{U}
 * Sorcery
 *
 * Search your library for three cards, exile them, then shuffle.
 * Draw a card.
 *
 * Composed from the atomic library pipeline: Gather (your library) → Select up to three
 * (you can find fewer if the library doesn't hold three) → Move to exile → shuffle → draw.
 */
val ManipulateFate = card("Manipulate Fate") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Search your library for three cards, exile them, then shuffle.\nDraw a card."

    spell {
        effect = Effects.Composite(
            listOf(
                // Search your library.
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Any),
                    storeAs = "searchable"
                ),
                // Choose up to three cards.
                SelectFromCollectionEffect(
                    from = "searchable",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(3)),
                    chooser = Chooser.Controller,
                    storeSelected = "exiled",
                    prompt = "Choose up to three cards to exile"
                ),
                // Exile them.
                MoveCollectionEffect(
                    from = "exiled",
                    destination = CardDestination.ToZone(Zone.EXILE, Player.You)
                ),
                // Then shuffle.
                ShuffleLibraryEffect(EffectTarget.Controller),
                // Draw a card.
                Effects.DrawCards(1)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "60"
        artist = "John Matson"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5bb52acb-dedb-4ed6-a6da-8c036f2b2958.jpg?1562913616"
    }
}
