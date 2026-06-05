// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CounterEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetSpell


/**
 * Mystic Denial
 * {1}{U}{U}
 * Instant
 * Counter target creature or sorcery spell.
 */
val MysticDenial = card("Mystic Denial") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    spell {
        val t = target("target", TargetSpell(filter = TargetFilter.CreatureOrSorcerySpellOnStack))
        effect = CounterEffect()
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "61"
        artist = "Hannibal King"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52d60f29-6da0-4ce6-9c92-96f313007271.jpg"
    }
}
