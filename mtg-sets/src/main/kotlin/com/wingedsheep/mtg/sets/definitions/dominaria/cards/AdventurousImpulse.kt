package com.wingedsheep.mtg.sets.definitions.dominaria.cards

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
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Adventurous Impulse
 * {G}
 * Sorcery
 * Look at the top three cards of your library. You may reveal a creature or land card
 * from among them and put it into your hand. Put the rest on the bottom of your library
 * in any order.
 */
val AdventurousImpulse = card("Adventurous Impulse") {
    manaCost = "{G}"
    typeLine = "Sorcery"
    oracleText = "Look at the top three cards of your library. You may reveal a creature or land card from among them and put it into your hand. Put the rest on the bottom of your library in any order."

    spell {
        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.Fixed(3)),
                    storeAs = "looked"
                ),
                SelectFromCollectionEffect(
                    from = "looked",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    filter = GameObjectFilter.Creature or GameObjectFilter.Land,
                    storeSelected = "kept",
                    storeRemainder = "rest",
                    prompt = "You may reveal a creature or land card and put it into your hand",
                    showAllCards = true
                ),
                MoveCollectionEffect(
                    from = "kept",
                    destination = CardDestination.ToZone(Zone.HAND)
                ),
                MoveCollectionEffect(
                    from = "rest",
                    destination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Titus Lunter"
        flavorText = "\"Every odyssey begins with a single step.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f426c92c-6e71-49f0-9a91-0d529bf8c17d.jpg?1562745600"
    }
}
