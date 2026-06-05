// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Fire Imp
 * {2}{R}
 * Creature — Imp
 * 2/1
 * When this creature enters, it deals 2 damage to target creature.
 */
val FireImp = card("Fire Imp") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Imp"
    power = 2
    toughness = 1
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = DealDamageEffect(2, t)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "126"
        artist = "DiTerlizzi"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea7edaf3-7941-4085-bdbc-e5c9832b6444.jpg"
    }
}
