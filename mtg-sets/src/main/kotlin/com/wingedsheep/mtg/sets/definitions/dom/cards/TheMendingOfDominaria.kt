package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.LibraryPatterns
import com.wingedsheep.sdk.dsl.Effects

/**
 * The Mending of Dominaria
 * {3}{G}{G}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I, II — Mill two cards, then you may return a creature card from your graveyard to your hand.
 * III — Return all land cards from your graveyard to the battlefield, then shuffle your graveyard
 *       into your library.
 */
val TheMendingOfDominaria = card("The Mending of Dominaria") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I, II — Mill two cards, then you may return a creature card from your graveyard to your hand.\n" +
        "III — Return all land cards from your graveyard to the battlefield, then shuffle your graveyard into your library."

    // Chapter I & II: Mill 2, then may return a creature card from graveyard to hand
    val chapterOneAndTwoEffect = LibraryPatterns.mill(2) then Effects.Composite(
        listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature),
                storeAs = "graveyardCreatures"
            ),
            SelectFromCollectionEffect(
                from = "graveyardCreatures",
                selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                storeSelected = "chosen",
                prompt = "You may return a creature card from your graveyard to your hand"
            ),
            MoveCollectionEffect(
                from = "chosen",
                destination = CardDestination.ToZone(Zone.HAND)
            )
        )
    )

    sagaChapter(1) {
        effect = chapterOneAndTwoEffect
    }

    sagaChapter(2) {
        effect = chapterOneAndTwoEffect
    }

    // Chapter III: Return all land cards from graveyard to battlefield, then shuffle graveyard into library
    sagaChapter(3) {
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Land),
                    storeAs = "graveyardLands"
                ),
                SelectFromCollectionEffect(
                    from = "graveyardLands",
                    selection = SelectionMode.All,
                    storeSelected = "allLands"
                ),
                MoveCollectionEffect(
                    from = "allLands",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                )
            )
        ) then LibraryPatterns.shuffleGraveyardIntoLibrary(EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "173"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d5e4d9a-34e1-46eb-814b-8e3bd4475b8a.jpg?1562731319"
    }
}
