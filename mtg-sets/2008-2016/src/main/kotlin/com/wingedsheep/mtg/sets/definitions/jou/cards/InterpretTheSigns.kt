package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Interpret the Signs
 * {5}{U}
 * Sorcery
 *
 * Scry 3, then reveal the top card of your library. Draw cards equal to that card's mana value.
 *
 * The revealed card stays on top of the library (CR 701.20b — revealing doesn't move it), so with
 * a nonzero mana value it is the first card drawn. The count is read before any card is drawn.
 */
val InterpretTheSigns = card("Interpret the Signs") {
    manaCost = "{5}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Scry 3, then reveal the top card of your library. Draw cards equal to that card's mana value. (To scry 3, look at the top three cards of your library, then put any number of them on the bottom and the rest on top in any order.)"

    spell {
        effect = Effects.Scry(3) then Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(1, Player.You))
            reveal(top)
            run(Effects.DrawCards(DynamicAmounts.manaValueOf(top)))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "Cynthia Sheppard"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/696f2da6-3943-4555-9610-e6b925b3d6bc.jpg?1783939447"
        ruling("2020-11-10", "If you reveal a card with mana value 0, such as a land card, you won't draw any cards. Otherwise, the card you revealed will be the first card you draw.")
        ruling("2020-11-10", "If a card in a player's library has {X} in its mana cost, X is considered to be 0.")
    }
}
