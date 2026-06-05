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
 * Charging Paladin
 * {2}{W}
 * Creature — Human Knight
 * 2/2
 * Whenever this creature attacks, it gets +0/+3 until end of turn.
 */
val ChargingPaladin = card("Charging Paladin") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = ModifyStatsEffect(powerModifier = 0, toughnessModifier = 3, target = EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "11"
        artist = "Kev Walker"
        flavorText = "A true warrior's thoughts are of victory, not death."
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29db1bbf-a6cf-460c-bec8-dbd682157af4.jpg"
    }
}
