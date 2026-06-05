// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Burning Cloak
 * {R}
 * Sorcery
 * Target creature gets +2/+0 until end of turn. Burning Cloak deals 2 damage to that creature.
 */
val BurningCloak = card("Burning Cloak") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = CompositeEffect(
        listOf(
            ModifyStatsEffect(powerModifier = 2, toughnessModifier = 0, target = t),
            DealDamageEffect(2, t)
        )
    )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2b8f443-dba5-45a5-bb2e-f57b4fdd1d01.jpg"
    }
}
