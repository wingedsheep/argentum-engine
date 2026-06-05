// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Seasoned Marshal
 * {2}{W}{W}
 * Creature — Human Soldier
 * 2/2
 * Whenever this creature attacks, you may tap target creature.
 */
val SeasonedMarshal = card("Seasoned Marshal") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    triggeredAbility {
        trigger = Triggers.Attacks
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = MayEffect(Effects.Tap(t))
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "Zina Saunders"
        imageUri = "https://cards.scryfall.io/normal/front/1/7/17db0060-3667-4c8c-ae9b-d62dceac64e3.jpg"
    }
}
