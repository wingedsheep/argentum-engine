package com.wingedsheep.mtg.sets.definitions.scg.cards

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
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

val LongTermPlans = card("Long-Term Plans") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Search your library for a card, then shuffle and put that card third from the top."

    spell {
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Any),
                    storeAs = "searchable"
                ),
                SelectFromCollectionEffect(
                    from = "searchable",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    storeSelected = "found"
                ),
                ShuffleLibraryEffect(),
                MoveCollectionEffect(
                    from = "found",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom)
                ),
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(2)),
                    storeAs = "aboveCards"
                ),
                MoveCollectionEffect(
                    from = "found",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top)
                ),
                MoveCollectionEffect(
                    from = "aboveCards",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Top)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "38"
        artist = "Ben Thompson"
        flavorText = "\"Wait, it'll come to me in a minute.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7e0422d9-9694-45b6-9c2b-2ca31198cebf.jpg?1562531196"
        ruling("2004-10-04", "If there are fewer than 3 cards in your library, put the card on the bottom of your library.")
    }
}
