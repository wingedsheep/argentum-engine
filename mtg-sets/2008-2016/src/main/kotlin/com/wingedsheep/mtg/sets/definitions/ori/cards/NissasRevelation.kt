package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nissa's Revelation
 * {5}{G}{G}
 * Sorcery
 * Scry 5, then reveal the top card of your library. If it's a creature card, you draw cards equal
 * to its power and you gain life equal to its toughness.
 *
 * Interpret the Signs' scry-then-reveal shape with Llanowar Empath's creature check. The revealed
 * card stays on top of the library (revealing doesn't move it), so with positive power it is the
 * first card drawn. Both amounts are read off the revealed card *before* the draw moves it: once
 * drawn it is a new object in a hidden zone, so its toughness is its last-known value from the
 * library (CR 608.2h) — which matters for a characteristic-defining P/T such as "equal to the
 * number of cards in your hand". Power or toughness of 0 or less draws nothing / gains nothing.
 */
val NissasRevelation = card("Nissa's Revelation") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Scry 5, then reveal the top card of your library. If it's a creature card, you " +
        "draw cards equal to its power and you gain life equal to its toughness."

    spell {
        effect = Effects.Scry(5) then Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(1, Player.You))
            reveal(top)
            ifNotEmpty(top, GameObjectFilter.Creature) {
                val power = storeNumber(DynamicAmounts.nonNegative(DynamicAmounts.powerOf(top.asTarget(0))))
                val toughness = storeNumber(DynamicAmounts.nonNegative(DynamicAmounts.toughnessOf(top.asTarget(0))))
                run(Effects.DrawCards(power.amount))
                run(Effects.GainLife(toughness.amount))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "191"
        artist = "Izzy"
        flavorText = "The evil inside the mountain pulsed at Nissa in a shock wave of madness."
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71143665-012f-4723-a58b-d5cfdbe82e8f.jpg?1783938319"
        ruling("2015-06-22", "If the top card of your library isn't a creature card or it's a creature card with power 0 or less, you won't draw any cards. Otherwise, the first card you draw will be the card you revealed.")
    }
}
