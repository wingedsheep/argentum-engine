package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Recall — Legends #70
 * {X}{X}{U} · Sorcery
 *
 * Discard X cards, then return a card from your graveyard to your hand for each card discarded
 * this way. Exile Recall.
 *
 * Gather → Select → Discard, then a fresh gather of the graveyard *after* the discard, so the
 * cards just discarded are eligible to come back (ruling). With fewer than X cards in hand the
 * select clamps to the whole hand, and the return count reads the discarded collection's size,
 * not X. All returned cards move to hand together. `selfExile()` sends Recall to exile instead
 * of the graveyard as it finishes resolving, so it can never return itself.
 */
val Recall = card("Recall") {
    manaCost = "{X}{X}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Discard X cards, then return a card from your graveyard to your hand for each " +
        "card discarded this way. Exile Recall."

    spell {
        selfExile()
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val discarded = chooseExactly(
                DynamicAmounts.xValue(), from = hand,
                prompt = "Choose cards to discard"
            )
            discard(discarded)
            val graveyard = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You))
            val returned = chooseExactly(
                discarded.count, from = graveyard,
                prompt = "Choose cards to return to your hand"
            )
            toHand(returned)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "70"
        artist = "Brian Snõddy"
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33296718-0625-4422-a65c-b21cf99c52ec.jpg?1783948072"
        ruling("2009-10-01", "You don't discard cards until Recall resolves. If you don't have X cards in your hand at that time, you discard all the cards in your hand.")
        ruling("2009-10-01", "You don't choose which cards in your graveyard you'll return to your hand until after you discard cards. You choose a card in your graveyard for each card you discarded, then you put all cards chosen this way into your hand at the same time. You may choose to return some or all of the cards you just discarded.")
    }
}
