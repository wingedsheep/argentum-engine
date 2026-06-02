package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.dsl.Effects

/**
 * Scouting Trek
 * {1}{G}
 * Sorcery
 *
 * Search your library for any number of basic land cards, reveal those cards,
 * then shuffle and put them on top.
 */
val ScoutingTrek = card("Scouting Trek") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Search your library for any number of basic land cards, reveal those cards, then shuffle and put them on top."

    spell {
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.LIBRARY, filter = Filters.BasicLand),
                    storeAs = "searchable"
                ),
                SelectFromCollectionEffect(
                    from = "searchable",
                    selection = SelectionMode.ChooseAnyNumber,
                    storeSelected = "found",
                    prompt = "Search your library for any number of basic land cards"
                ),
                RevealCollectionEffect(from = "found"),
                ShuffleLibraryEffect(),
                MoveCollectionEffect(
                    from = "found",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "210"
        artist = "Stephanie Law"
        flavorText = "\"I have chosen my path. Who will walk it with me?\"\n—Eladamri"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b882e68-5c03-4ec6-9982-8c3b09847969.jpg?1562900439"
    }
}
