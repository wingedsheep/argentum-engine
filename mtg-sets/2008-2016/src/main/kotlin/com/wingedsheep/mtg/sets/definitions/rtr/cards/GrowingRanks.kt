package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Growing Ranks
 * {2}{G/W}{G/W}
 * Enchantment
 *
 * At the beginning of your upkeep, populate. (Create a token that's a copy of a creature token you
 * control.)
 *
 * Populate chooses — never targets — a creature token you control on resolution, so it is
 * [Effects.CreateTokenCopyOfChosenPermanent] over creature tokens, as on Coursers' Accord. With no
 * creature token, the trigger does nothing.
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 */
val GrowingRanks = card("Growing Ranks") {
    manaCost = "{2}{G/W}{G/W}"
    colorIdentity = "GW"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, populate. (Create a token that's a copy of a " +
        "creature token you control.)"

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.CreateTokenCopyOfChosenPermanent(GameObjectFilter.Creature.token())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "217"
        artist = "Seb McKinnon"
        flavorText = "\"We will grow an army large enough to withstand the Izzet's madness.\"\n—Trostani"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/12f31616-1249-4964-b81a-4435405a2449.jpg?1783940327"
    }
}
