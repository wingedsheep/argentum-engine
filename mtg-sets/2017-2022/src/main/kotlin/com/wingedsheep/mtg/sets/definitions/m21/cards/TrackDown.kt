package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Track Down
 * {1}{G}
 * Sorcery
 * Scry 3, then reveal the top card of your library. If it's a creature or land card, draw a card.
 *
 * Invasion of Pyrulea's pipeline: scry, reveal the top card, and a real draw (the revealed card is
 * the one drawn) when it matches; otherwise it stays on top (rulings).
 */
val TrackDown = card("Track Down") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Scry 3, then reveal the top card of your library. If it's a creature or land card, " +
        "draw a card. (To scry 3, look at the top three cards of your library, then put any number of " +
        "them on the bottom and the rest on top in any order.)"

    spell {
        effect = Effects.Pipeline {
            run(Patterns.Library.scry(3))
            val revealed = gather(CardSource.TopOfLibrary(1), revealed = true)
            run(Effects.If(
                condition = whenMatches(revealed, GameObjectFilter.Creature or GameObjectFilter.Land),
                then = Effects.DrawCards(1),
            ))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "211"
        artist = "Scott Murphy"
        flavorText = "Scents fade and footprints wash away, but some trails can never be erased."
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b41b6c8a-bd76-4b69-ae72-4c8acac379b4.jpg?1783930664"
        ruling("2020-06-23", "The card you reveal will be the card you draw, if applicable.")
        ruling("2020-06-23", "If you don't draw the card you reveal, it remains on top of your library.")
    }
}
