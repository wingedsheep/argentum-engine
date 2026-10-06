package com.wingedsheep.mtg.sets.definitions.ala.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Gift of the Gargantuan
 * {2}{G}
 * Sorcery
 * Look at the top four cards of your library. You may reveal a creature card and/or a land card
 * from among them and put the revealed cards into your hand. Put the rest on the bottom of your
 * library in any order.
 *
 * Canonical printing: Shards of Alara, the card's earliest real printing.
 *
 * "A creature card and/or a land card" is two independent up-to-one picks — a creature card first,
 * then a land card from what's left — rather than one restricted two-card selection. Picking in
 * sequence is what makes the Dryad Arbor ruling fall out: a creature land can be the creature pick
 * (leaving room for another land) or the land pick (after another creature), but never both.
 */
val GiftOfTheGargantuan = card("Gift of the Gargantuan") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Look at the top four cards of your library. You may reveal a creature card and/or a land card from among them and put the revealed cards into your hand. Put the rest on the bottom of your library in any order."

    spell {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(4))
            val (creature, afterCreature) = chooseUpToSplit(
                1, from = looked,
                filter = GameObjectFilter.Creature,
                prompt = "You may reveal a creature card to put into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Leave",
                showAllCards = true
            )
            val (land, rest) = chooseUpToSplit(
                1, from = afterCreature,
                filter = GameObjectFilter.Land,
                prompt = "You may reveal a land card to put into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom",
                showAllCards = true
            )
            toHand(creature, revealed = true)
            toHand(land, revealed = true)
            toLibraryBottom(rest)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "132"
        artist = "Jean-Sébastien Rossbach"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/1850be87-54de-49b3-a407-6fb2b278b25c.jpg?1783942554"
        ruling("2008-10-01", "You may reveal a creature card, or you may reveal a land card, or you may reveal both a creature card and a land card.")
        ruling("2008-10-01", "If one of the cards is Dryad Arbor (a creature land card), you may reveal Dryad Arbor and another land card or Dryad Arbor and another creature card.")
    }
}
