package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Wasteful Harvest
 * {2}{G}
 * Instant
 *
 * Mill five cards. You may put a permanent card from among the cards milled this way into
 * your hand.
 */
val WastefulHarvest = card("Wasteful Harvest") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Mill five cards. You may put a permanent card from among the cards milled this way into your hand. (To mill a card, put the top card of your library into your graveyard.)"

    spell {
        effect = Effects.Pipeline {
            val milled = mill(5)
            val picked = chooseUpTo(
                1,
                from = milled,
                filter = GameObjectFilter.Permanent,
                showAllCards = true,
                prompt = "You may put a permanent card milled this way into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(picked)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "196"
        artist = "Alex Stone"
        flavorText = "Once the brothers discovered the pristine island of Argoth, the war came down to who could plunder it faster."
        imageUri = "https://cards.scryfall.io/normal/front/0/0/00a0c819-36d1-442b-bec5-e6df58a122c0.jpg?1783920037"
    }
}
