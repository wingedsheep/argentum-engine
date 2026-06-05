// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.YouWereAttackedThisStep
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget


/**
 * Treetop Defense
 * {1}{G}
 * Instant
 * Cast this spell only during the declare attackers step and only if you've been attacked this step.
 * Creatures you control gain reach until end of turn. (They can block creatures with flying.)
 */
val TreetopDefense = card("Treetop Defense") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    spell {
        castOnlyDuring(Step.DECLARE_ATTACKERS)
        castOnlyIf(YouWereAttackedThisStep)
        effect = ForEachInGroupEffect(GroupFilter(GameObjectFilter.Creature.youControl()), GrantKeywordEffect(Keyword.REACH, EffectTarget.Self))
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "190"
        artist = "Zina Saunders"
        imageUri = "https://cards.scryfall.io/normal/front/f/5/f5e134b3-e8af-41e9-928d-c217ea7b2b13.jpg"
    }
}
