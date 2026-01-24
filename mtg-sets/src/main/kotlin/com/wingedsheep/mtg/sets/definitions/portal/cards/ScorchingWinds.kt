package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageToAttackingCreaturesEffect
import com.wingedsheep.sdk.scripting.YouWereAttackedThisStep

/**
 * Scorching Winds
 * {R}
 * Instant
 * Cast this spell only during the declare attackers step and only if you've been attacked this step.
 * Scorching Winds deals 1 damage to each attacking creature.
 */
val ScorchingWinds = card("Scorching Winds") {
    manaCost = "{R}"
    typeLine = "Instant"

    spell {
        castOnlyDuring(Step.DECLARE_ATTACKERS)
        castOnlyIf(YouWereAttackedThisStep)
        effect = DealDamageToAttackingCreaturesEffect(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "D. Alexander Gregory"
        flavorText = "The desert wind carries fire in its breath."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c3d0e1f2-a3b4-c5d6-e7f8-a9b0c1d2e3f4.jpg"
    }
}
