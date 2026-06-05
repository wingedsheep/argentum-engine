// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect


/**
 * Thing from the Deep
 * {6}{U}{U}{U}
 * Creature — Leviathan
 * 9/9
 * Whenever this creature attacks, sacrifice it unless you sacrifice an Island.
 */
val ThingfromtheDeep = card("Thing from the Deep") {
    manaCost = "{6}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Leviathan"
    power = 9
    toughness = 9
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = PayOrSufferEffect(cost = PayCost.Sacrifice(GameObjectFilter.Land.withSubtype("Island")), suffer = SacrificeSelfEffect)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "73"
        artist = "Paolo Parente"
        flavorText = "Seafarers fear sailing off the world's edge not so much as down its gullet."
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb3b9682-7f3a-4857-9ecf-01f3530659fc.jpg"
    }
}
