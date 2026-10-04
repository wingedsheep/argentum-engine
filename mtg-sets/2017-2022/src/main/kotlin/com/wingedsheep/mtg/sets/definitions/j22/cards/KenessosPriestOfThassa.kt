package com.wingedsheep.mtg.sets.definitions.j22.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyScryAmount
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Kenessos, Priest of Thassa
 * {1}{U}
 * Legendary Creature — Merfolk Cleric
 * 1/3
 *
 * If you would scry a number of cards, scry that many cards plus one instead.
 * {3}{G/U}: Look at the top card of your library. If it's a Kraken, Leviathan, Octopus, or Serpent
 * creature card, you may put it onto the battlefield. If you don't put the card onto the
 * battlefield, you may put it on the bottom of your library.
 *
 * Notes:
 *  - The scry replacement is [ModifyScryAmount], applied at the scry announcement: the look, the
 *    top/bottom choice and the "whenever you scry" count all see N + 1. A scry 0 is no scry event
 *    (CR 701.22b) and stays 0.
 *  - The activated ability is Bucolic Ranch's shape with the battlefield as the first destination;
 *    leaving the card on top is allowed (Scryfall ruling).
 */
val KenessosPriestOfThassa = card("Kenessos, Priest of Thassa") {
    manaCost = "{1}{U}"
    typeLine = "Legendary Creature — Merfolk Cleric"
    power = 1
    toughness = 3
    oracleText = "If you would scry a number of cards, scry that many cards plus one instead.\n" +
        "{3}{G/U}: Look at the top card of your library. If it's a Kraken, Leviathan, Octopus, or " +
        "Serpent creature card, you may put it onto the battlefield. If you don't put the card onto " +
        "the battlefield, you may put it on the bottom of your library."

    // If you would scry a number of cards, scry that many cards plus one instead.
    replacementEffect(ModifyScryAmount(modifier = 1))

    // {3}{G/U}: Look at the top card of your library. …
    activatedAbility {
        cost = Costs.Mana("{3}{G/U}")
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1))
            // If it's a Kraken, Leviathan, Octopus, or Serpent creature card, you may put it
            // onto the battlefield.
            val (kept, rest) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Creature.withAnySubtype("Kraken", "Leviathan", "Octopus", "Serpent"),
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Leave"
            )
            move(kept, CardDestination.ToZone(Zone.BATTLEFIELD))
            // If you don't, you may put it on the bottom of your library (else it stays on top).
            val toBottom = chooseUpTo(
                1,
                from = rest,
                selectedLabel = "Put on bottom",
                remainderLabel = "Leave on top"
            )
            toLibraryBottom(toBottom, order = CardOrder.Preserve)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "13"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0ba97da8-0106-4e8b-b58b-8f2d63e3d618.jpg?1783919191"
    }
}
