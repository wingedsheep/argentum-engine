// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount


/**
 * Blaze
 * {X}{R}
 * Sorcery
 * Blaze deals X damage to any target.
 */
val Blaze = card("Blaze") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    spell {
        val t = target("target", AnyTarget())
        effect = DealDamageEffect(DynamicAmount.XValue, t)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Gerry Grace"
        flavorText = "Fire never dies alone."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f175c959-3b5d-46a3-9194-fad2359bbff9.jpg"
    }
}
