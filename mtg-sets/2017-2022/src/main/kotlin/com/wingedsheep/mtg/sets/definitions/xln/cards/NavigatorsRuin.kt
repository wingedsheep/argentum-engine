package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Navigator's Ruin
 * {2}{U}
 * Enchantment
 * Raid — At the beginning of your end step, if you attacked this turn, target opponent mills four cards.
 *
 * Raid is the intervening-if [Conditions.YouAttackedThisTurn] on the end-step trigger
 * (same shape as Perforating Artist); the payoff is a targeted mill of four.
 */
val NavigatorsRuin = card("Navigator's Ruin") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Raid — At the beginning of your end step, if you attacked this turn, " +
        "target opponent mills four cards."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouAttackedThisTurn
        val opponent = target(Targets.Opponent)
        effect = Patterns.Library.mill(4, opponent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Zoltan Boros"
        flavorText = "\"You won't be needing this. Or this. Or these . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb082e5e-770a-4c5a-8976-ee394ab73c86.jpg?1783935779"
    }
}
