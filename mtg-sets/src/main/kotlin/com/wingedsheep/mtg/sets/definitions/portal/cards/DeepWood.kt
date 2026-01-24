package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.PreventDamageFromAttackingCreaturesThisTurnEffect
import com.wingedsheep.sdk.scripting.YouWereAttackedThisStep

/**
 * Deep Wood
 * {1}{G}
 * Instant
 * Cast this spell only during the declare attackers step and only if you've been attacked this step.
 * Prevent all damage that would be dealt to you this turn by attacking creatures.
 */
val DeepWood = card("Deep Wood") {
    manaCost = "{1}{G}"
    typeLine = "Instant"

    spell {
        castOnlyDuring(Step.DECLARE_ATTACKERS)
        castOnlyIf(YouWereAttackedThisStep)
        effect = PreventDamageFromAttackingCreaturesThisTurnEffect
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "162"
        artist = "Terese Nielsen"
        flavorText = "In the deep wood, the trees themselves offer protection."
        imageUri = "https://cards.scryfall.io/normal/front/0/0/004eb8ea-8e9a-4aff-8f41-e7f2c2f0d68c.jpg"
    }
}
