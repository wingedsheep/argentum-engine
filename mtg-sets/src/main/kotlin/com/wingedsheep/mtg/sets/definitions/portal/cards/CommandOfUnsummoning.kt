package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ReturnToHandEffect
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.scripting.YouWereAttackedThisStep
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Command of Unsummoning
 * {2}{U}
 * Instant
 * Cast this spell only during the declare attackers step and only if you've been attacked this step.
 * Return one or two target attacking creatures to their owners' hands.
 */
val CommandOfUnsummoning = card("Command of Unsummoning") {
    manaCost = "{2}{U}"
    typeLine = "Instant"

    spell {
        castOnlyDuring(Step.DECLARE_ATTACKERS)
        castOnlyIf(YouWereAttackedThisStep)

        target = TargetCreature(
            count = 2,
            minCount = 1,
            filter = TargetFilter.AttackingCreature
        )
        effect = ReturnToHandEffect(EffectTarget.ContextTarget(0)) then
                ReturnToHandEffect(EffectTarget.ContextTarget(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Phil Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e61b97fc-fa42-40a6-918e-e06383bfcae3.jpg"
        ruling(
            "10/4/2004",
            "This card was originally printed as a sorcery and has received errata to make it an instant."
        )
    }
}
