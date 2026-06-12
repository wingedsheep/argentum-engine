package com.wingedsheep.mtg.sets.definitions.dom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns

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
    val chapterOneAndTwoEffect = Patterns.Library.mill(2) then Effects.Pipeline {
        val graveyardCreatures = gather(
            CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature),
            name = "graveyardCreatures"
        )
        val chosen = chooseUpTo(
            1, from = graveyardCreatures,
            prompt = "You may return a creature card from your graveyard to your hand",
            name = "chosen"
        )
        move(
            chosen,
            destination = CardDestination.ToZone(Zone.HAND)
        )
    }

    sagaChapter(1) {
        effect = chapterOneAndTwoEffect
    }

    sagaChapter(2) {
        effect = chapterOneAndTwoEffect
    }

    // Chapter III: Return all land cards from graveyard to battlefield, then shuffle graveyard into library
    sagaChapter(3) {
        effect = Effects.Pipeline {
            val graveyardLands = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Land),
                name = "graveyardLands"
            )
            val allLands = selectAll(
                from = graveyardLands,
                name = "allLands"
            )
            move(
                allLands,
                destination = CardDestination.ToZone(Zone.BATTLEFIELD)
            )
        } then Patterns.Library.shuffleGraveyardIntoLibrary(EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "173"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d5e4d9a-34e1-46eb-814b-8e3bd4475b8a.jpg?1562731319"
    }
}
