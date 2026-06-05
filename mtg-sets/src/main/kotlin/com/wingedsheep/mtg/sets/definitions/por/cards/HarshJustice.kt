// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.YouWereAttackedThisStep
import com.wingedsheep.sdk.scripting.effects.ReflectCombatDamageEffect


/**
 * Harsh Justice
 * {2}{W}
 * Instant
 * Cast this spell only during the declare attackers step and only if you've been attacked this step.
 * This turn, whenever an attacking creature deals combat damage to you, it deals that much damage to its controller.
 */
val HarshJustice = card("Harsh Justice") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    spell {
        castOnlyDuring(Step.DECLARE_ATTACKERS)
        castOnlyIf(YouWereAttackedThisStep)
        effect = ReflectCombatDamageEffect()
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "John Coulthart"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/3657001a-7f79-4d3f-9d35-462ecf684fa8.jpg"
    }
}
