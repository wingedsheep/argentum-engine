// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MustBeBlockedEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Alluring Scent
 * {1}{G}{G}
 * Sorcery
 * All creatures able to block target creature this turn do so.
 */
val AlluringScent = card("Alluring Scent") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = MustBeBlockedEffect(t)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "157"
        artist = "Ted Naifeh"
        flavorText = "Doom rarely smells this sweet."
        imageUri = "https://cards.scryfall.io/normal/front/8/7/8726242e-bfd8-4ed5-a016-ac0c82e4762b.jpg"
    }
}
