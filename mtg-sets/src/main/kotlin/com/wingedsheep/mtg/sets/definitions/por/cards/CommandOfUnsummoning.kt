// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.YouWereAttackedThisStep
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Command of Unsummoning
 * {2}{U}
 * Instant
 * Cast this spell only during the declare attackers step and only if you've been attacked this step.
 * Return one or two target attacking creatures to their owner's hand.
 */
val CommandofUnsummoning = card("Command of Unsummoning") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    spell {
        castOnlyDuring(Step.DECLARE_ATTACKERS)
        castOnlyIf(YouWereAttackedThisStep)
        val t = target("target", TargetCreature(count = 2, minCount = 1, filter = TargetFilter.Creature.attacking()))
        effect = ForEachTargetEffect(listOf(MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.HAND)))
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Phil Foglio"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e61b97fc-fa42-40a6-918e-e06383bfcae3.jpg"
    }
}
