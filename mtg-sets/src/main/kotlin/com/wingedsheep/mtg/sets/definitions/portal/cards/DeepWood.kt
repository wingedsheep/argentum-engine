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
        imageUri = "https://cards.scryfall.io/normal/front/4/2/4216c5aa-9df0-4e7c-b3e9-a3f712b17ce7.jpg"
        ruling(
            "10/4/2004",
            "This card was originally printed as a sorcery and has received errata to make it an instant."
        )
    }
}
