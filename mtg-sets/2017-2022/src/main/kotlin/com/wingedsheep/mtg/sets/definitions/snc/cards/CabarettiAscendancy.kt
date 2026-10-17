package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Cabaretti Ascendancy
 * {R}{G}{W}
 * Enchantment
 *
 * At the beginning of your upkeep, look at the top card of your library. If it's a creature or
 * planeswalker card, you may reveal it and put it into your hand. If you don't put the card into
 * your hand, you may put it on the bottom of your library.
 *
 * Modeling notes:
 *  - Same look-then-optionally-bottom pipeline as Bucolic Ranch: a filtered up-to-one split for
 *    the reveal-to-hand, then an unfiltered up-to-one choice for the bottom; an unchosen card
 *    stays on top.
 */
val CabarettiAscendancy = card("Cabaretti Ascendancy") {
    manaCost = "{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, look at the top card of your library. If it's a " +
        "creature or planeswalker card, you may reveal it and put it into your hand. If you don't put " +
        "the card into your hand, you may put it on the bottom of your library."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(1))
            val (toHandCards, notTaken) = chooseUpToSplit(
                1,
                from = looked,
                filter = GameObjectFilter.Creature or GameObjectFilter.Planeswalker,
                prompt = "Reveal the creature or planeswalker card and put it into your hand?",
                selectedLabel = "Reveal and put into your hand",
                remainderLabel = "Leave it"
            )
            toHand(toHandCards, revealed = true)
            val toBottom = chooseUpTo(
                1,
                from = notTaken,
                selectedLabel = "Put on bottom",
                remainderLabel = "Leave on top"
            )
            toLibraryBottom(toBottom, order = CardOrder.Preserve)
        }
        description = "At the beginning of your upkeep, look at the top card of your library. If it's " +
            "a creature or planeswalker card, you may reveal it and put it into your hand. If you don't " +
            "put the card into your hand, you may put it on the bottom of your library."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "172"
        artist = "Kari Christensen"
        flavorText = "\"Whatever you're looking for—food, family, Halo—we've got the best in town.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/7/9738d616-b38f-4968-89d6-805ae368e2d4.jpg?1783923094"
    }
}
