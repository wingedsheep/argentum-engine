// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.YouWereAttackedThisStep
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget


/**
 * Scorching Winds
 * {R}
 * Instant
 * Cast this spell only during the declare attackers step and only if you've been attacked this step.
 * Scorching Winds deals 1 damage to each attacking creature.
 */
val ScorchingWinds = card("Scorching Winds") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    spell {
        castOnlyDuring(Step.DECLARE_ATTACKERS)
        castOnlyIf(YouWereAttackedThisStep)
        effect = ForEachInGroupEffect(GroupFilter(GameObjectFilter.Creature.attacking()), DealDamageEffect(1, EffectTarget.Self))
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "D. Alexander Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5fec371e-d4ba-439f-b1b8-2aac3f5b36bf.jpg"
    }
}
