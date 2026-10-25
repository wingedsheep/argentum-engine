package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Tasha's Hideous Laughter — Adventures in the Forgotten Realms #78
 * {1}{U}{U} · Sorcery
 *
 * Each opponent exiles cards from the top of their library until that player has exiled cards
 * with total mana value 20 or greater.
 *
 * Dream Harvest's exile half with a threshold of 20 and no follow-up: each opponent's tally is
 * kept separately, and a library that runs out first is exiled whole.
 */
val TashasHideousLaughter = card("Tasha's Hideous Laughter") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Each opponent exiles cards from the top of their library until that player has exiled cards with total mana value 20 or greater."

    spell {
        effect = Effects.Pipeline {
            runStoringCollection {
                Effects.ExileLibraryUntilManaValue(players = Player.EachOpponent, threshold = 20, storeAs = it)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "78"
        artist = "Ilse Gort"
        flavorText = "The raging behir reared back, opened its terrible maw . . . and collapsed in a fit of horrifying giggles."
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4932113-904f-427a-9566-509cc008f3ef.jpg?1783926505"
    }
}
