package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Zone
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
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Scout the Borders
 * {2}{G}
 * Sorcery
 * Reveal the top five cards of your library. You may put a creature or land card
 * from among them into your hand. Put the rest into your graveyard.
 */
val ScoutTheBorders = card("Scout the Borders") {
    manaCost = "{2}{G}"
    typeLine = "Sorcery"
    oracleText = "Reveal the top five cards of your library. You may put a creature or land card from among them into your hand. Put the rest into your graveyard."

    spell {
        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(5)),
                    storeAs = "revealed",
                    revealed = true
                ),
                SelectFromCollectionEffect(
                    from = "revealed",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    filter = GameObjectFilter.Companion.CreatureOrLand,
                    storeSelected = "kept",
                    storeRemainder = "rest",
                    selectedLabel = "Put in hand",
                    remainderLabel = "Put in graveyard",
                    showAllCards = true
                ),
                MoveCollectionEffect(
                    from = "kept",
                    destination = CardDestination.ToZone(Zone.HAND)
                ),
                MoveCollectionEffect(
                    from = "rest",
                    destination = CardDestination.ToZone(Zone.GRAVEYARD)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "148"
        artist = "James Paick"
        flavorText = "\"I am in my element: the element of surprise.\" —Mogai, Sultai scout"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb15c6c7-8fe6-496c-9977-3e7942b920c4.jpg?1562795470"
    }
}
