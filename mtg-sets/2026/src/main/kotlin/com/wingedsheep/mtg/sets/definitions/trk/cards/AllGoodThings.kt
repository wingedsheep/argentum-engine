package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * All Good Things...
 * {3}{G}{U}
 * Enchantment — Saga
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)
 * I, II, III — Draw two cards, then exile a card from your hand.
 * IV — If all cards exiled with this Saga share a card type, you gain 10 life.
 *
 * Each of chapters I–III links the card it exiles to the Saga, so chapter IV reads the Saga's whole
 * linked-exile pile and asks whether one card type is common to every card in it.
 */
val AllGoodThings = card("All Good Things...") {
    manaCost = "{3}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)\n" +
        "I, II, III — Draw two cards, then exile a card from your hand.\n" +
        "IV — If all cards exiled with this Saga share a card type, you gain 10 life."

    val drawThenExile = Effects.DrawCards(2) then Effects.Pipeline {
        val hand = gather(CardSource.FromZone(Zone.HAND))
        val chosen = chooseExactly(1, hand, prompt = "Choose a card to exile")
        exile(chosen, linkToSource = true)
    }

    sagaChapter(1) { effect = drawThenExile }
    sagaChapter(2) { effect = drawThenExile }
    sagaChapter(3) { effect = drawThenExile }

    sagaChapter(4) {
        effect = Effects.Pipeline {
            val exiledWithSaga = gather(CardSource.FromLinkedExile())
            run(
                Effects.If(
                    condition = Conditions.AllInCollectionShareCardType(exiledWithSaga),
                    then = Effects.GainLife(10),
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "226"
        artist = "Lazare Gvimradze"
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe5dda31-159c-49c7-962e-a04b771256ea.jpg?1792753779"
    }
}
