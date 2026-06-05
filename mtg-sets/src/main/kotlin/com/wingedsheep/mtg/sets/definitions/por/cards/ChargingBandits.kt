// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget


/**
 * Charging Bandits
 * {4}{B}
 * Creature — Human Rogue
 * 3/3
 * Whenever this creature attacks, it gets +2/+0 until end of turn.
 */
val ChargingBandits = card("Charging Bandits") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 3
    toughness = 3
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = ModifyStatsEffect(powerModifier = 2, toughnessModifier = 0, target = EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "84"
        artist = "Dermot Power"
        flavorText = "The fear in their victims' eyes is their most cherished reward."
        imageUri = "https://cards.scryfall.io/normal/front/1/7/1721ee11-c7ee-4878-b2ab-4f090e0c5def.jpg"
    }
}
