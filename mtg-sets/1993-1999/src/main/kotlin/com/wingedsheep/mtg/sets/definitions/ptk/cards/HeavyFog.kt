package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.YouWereAttackedThisStep

/**
 * Heavy Fog
 * {1}{G}
 * Instant
 * Cast this spell only during the declare attackers step and only if you've been attacked this step.
 * Prevent all damage that would be dealt to you this turn by attacking creatures.
 */
val HeavyFog = card("Heavy Fog") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText =
        "Cast this spell only during the declare attackers step and only if you've been attacked this step.\n" +
        "Prevent all damage that would be dealt to you this turn by attacking creatures."

    spell {
        castOnlyDuring(Step.DECLARE_ATTACKERS)
        castOnlyIf(YouWereAttackedThisStep)
        effect = Effects.PreventDamageFromAttackingCreatures()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Liu Shangying"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/499fee7b-1942-4eb9-b1d0-806a1f6c0cd8.jpg"
    }
}
