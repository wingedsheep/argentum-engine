package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageToGroupEffect
import com.wingedsheep.sdk.scripting.GroupFilter
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
        effect = DealDamageToGroupEffect(1, GroupFilter.AttackingCreatures)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "D. Alexander Gregory"
        flavorText = "The desert wind carries fire in its breath."
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5fec371e-d4ba-439f-b1b8-2aac3f5b36bf.jpg"
        ruling(
            "10/4/2004",
            "This card was originally printed as a sorcery and has received errata to make it an instant."
        )
    }
}
