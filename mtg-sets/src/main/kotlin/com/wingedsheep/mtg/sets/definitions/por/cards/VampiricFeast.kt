// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget


/**
 * Vampiric Feast
 * {5}{B}{B}
 * Sorcery
 * Vampiric Feast deals 4 damage to any target and you gain 4 life.
 */
val VampiricFeast = card("Vampiric Feast") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    spell {
        val t = target("target", AnyTarget())
        effect = CompositeEffect(
        listOf(
            DealDamageEffect(4, t),
            GainLifeEffect(4)
        )
    )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "D. Alexander Gregory"
        flavorText = "It's not always gold the thief is after."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19500ffb-bfad-46d6-8a6e-d134405959c0.jpg"
    }
}
