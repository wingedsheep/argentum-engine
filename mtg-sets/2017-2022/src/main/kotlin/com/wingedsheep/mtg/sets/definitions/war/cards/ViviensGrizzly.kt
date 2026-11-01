package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Vivien's Grizzly — War of the Spark #182
 * {2}{G} · Creature — Bear Spirit · 2/3
 *
 * {3}{G}: Look at the top card of your library. If it's a creature or planeswalker card, you may
 * reveal it and put it into your hand. If you don't put the card into your hand, put it on the
 * bottom of your library.
 *
 * The standard peek pipeline: gather the top card (a look, not a move), offer up to one creature
 * or planeswalker card to reveal into hand, and send whatever wasn't taken — matching or not — to
 * the bottom of the library.
 */
val ViviensGrizzly = card("Vivien's Grizzly") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Bear Spirit"
    power = 2
    toughness = 3
    oracleText = "{3}{G}: Look at the top card of your library. If it's a creature or planeswalker " +
        "card, you may reveal it and put it into your hand. If you don't put the card into your " +
        "hand, put it on the bottom of your library."

    activatedAbility {
        cost = Costs.Mana("{3}{G}")
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1))
            val (toHandCards, notTaken) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.CreatureOrPlaneswalker,
                prompt = "Reveal the card and put it into your hand?",
                selectedLabel = "Reveal and put into your hand",
                remainderLabel = "Put on the bottom of your library"
            )
            toHand(toHandCards, revealed = true)
            toLibraryBottom(notTaken, order = CardOrder.Preserve)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "182"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37f2571b-6756-462c-9e71-3121fa458160.jpg?1783933403"
    }
}
