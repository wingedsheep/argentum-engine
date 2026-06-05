// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Soul Shred
 * {3}{B}{B}
 * Sorcery
 * Soul Shred deals 3 damage to target nonblack creature. You gain 3 life.
 */
val SoulShred = card("Soul Shred") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature.notColor(Color.BLACK)))
        effect = CompositeEffect(
        listOf(
            DealDamageEffect(3, t),
            GainLifeEffect(3)
        )
    )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "112"
        artist = "Alan Rabinowitz"
        flavorText = "It would be a shame to let life slip away to nothing."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/990902d2-9594-4963-807c-48a90324d487.jpg"
    }
}
