package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Forever Young
 * {1}{B}
 * Sorcery
 * Put any number of target creature cards from your graveyard on top of your library.
 * Draw a card.
 *
 * Footbottom Feast's script at sorcery speed: one `unlimited = true` target slot (zero targets is
 * legal, leaving a plain cantrip), the chosen cards moved to the top of your library in an order
 * you choose, then the draw — so the card you put on top is the card you draw.
 */
val ForeverYoung = card("Forever Young") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Put any number of target creature cards from your graveyard on top of your library.\n" +
        "Draw a card."

    spell {
        targets(TargetFilter.CreatureInYourGraveyard, unlimited = true)
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            toLibraryTop(chosen)
            run(Effects.DrawCards(1))
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "89"
        artist = "Volkan Baǵa"
        flavorText = "Those who say you only live once have never been touched by the Cauldron of Eternity."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/7873c1f9-572c-4740-82f8-cf3cbc7318d0.jpg?1783932639"
        ruling(
            "2019-10-04",
            "If you choose any target creature cards and they are all illegal targets by the time " +
                "Forever Young tries to resolve, the spell won't resolve. You won't draw a card."
        )
        ruling(
            "2019-10-04",
            "You choose the order that the creature cards are put on top of your library. You don't " +
                "have to reveal this order to other players. Whichever ends up on top will be the card you draw."
        )
        ruling("2019-10-04", "You can cast Forever Young with no targets if you just want to draw a card.")
    }
}
