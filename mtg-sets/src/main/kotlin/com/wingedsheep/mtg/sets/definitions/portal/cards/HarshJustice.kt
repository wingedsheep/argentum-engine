package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ReflectCombatDamageEffect
import com.wingedsheep.sdk.scripting.conditions.YouWereAttackedThisStep

/**
 * Harsh Justice
 * {2}{W}
 * Instant
 * Cast this spell only during the declare attackers step and only if you've been attacked this step.
 * This turn, whenever an attacking creature deals combat damage to you, it deals that
 * much damage to its controller.
 */
val HarshJustice = card("Harsh Justice") {
    manaCost = "{2}{W}"
    typeLine = "Instant"

    spell {
        // Cast restrictions
        castOnlyDuring(Step.DECLARE_ATTACKERS)
        castOnlyIf(YouWereAttackedThisStep)

        effect = ReflectCombatDamageEffect()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "John Coulthart"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/3657001a-7f79-4d3f-9d35-462ecf684fa8.jpg"
        ruling(
            "10/4/2004",
            "This card was originally printed as a sorcery and has received errata to make it an instant."
        )
    }
}
