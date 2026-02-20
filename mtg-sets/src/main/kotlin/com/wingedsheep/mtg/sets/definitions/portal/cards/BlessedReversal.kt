package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.conditions.YouWereAttackedThisStep

/**
 * Blessed Reversal
 * {1}{W}
 * Instant
 * Cast this spell only during the declare attackers step and only if you've been attacked this step.
 * You gain 3 life for each creature attacking you.
 */
val BlessedReversal = card("Blessed Reversal") {
    manaCost = "{1}{W}"
    typeLine = "Instant"

    spell {
        // Cast restrictions
        castOnlyDuring(Step.DECLARE_ATTACKERS)
        castOnlyIf(YouWereAttackedThisStep)

        effect = GainLifeEffect(DynamicAmounts.creaturesAttackingYou(multiplier = 3))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Zina Saunders"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/899ecc19-8106-4e5a-bb25-aaea9684ba0e.jpg"
        ruling(
            "10/4/2004",
            "This card was originally printed as a sorcery and has received errata to make it an instant."
        )
    }
}
