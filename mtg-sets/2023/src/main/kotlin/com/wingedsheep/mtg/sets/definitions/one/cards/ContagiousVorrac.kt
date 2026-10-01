package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Contagious Vorrac
 * {2}{G}
 * Creature — Phyrexian Boar Beast
 * 3/3
 *
 * When this creature enters, look at the top four cards of your library. You may reveal a land
 * card from among them and put it into your hand. Put the rest on the bottom of your library in a
 * random order. If you didn't put a card into your hand this way, proliferate.
 *
 * The optional `chooseUpToSplit(1)` is the "you may"; the proliferate branch keys off the kept
 * pile being empty — declined, or no land among the four.
 */
val ContagiousVorrac = card("Contagious Vorrac") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Phyrexian Boar Beast"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, look at the top four cards of your library. You may reveal a land card " +
        "from among them and put it into your hand. Put the rest on the bottom of your library in a random order. " +
        "If you didn't put a card into your hand this way, proliferate. " +
        "(Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(4))
            val (kept, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Land,
                prompt = "You may reveal a land card and put it into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Put on bottom",
                showAllCards = true
            )
            toHand(kept, revealed = true)
            toLibraryBottom(rest, order = CardOrder.Random)
            ifNotEmpty(kept) {
                run(Effects.Nothing)
            } orElse {
                run(Effects.Proliferate())
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Maxime Minard"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18af2c85-e58f-4043-99d3-e90121348aca.jpg?1783918018"
    }
}
