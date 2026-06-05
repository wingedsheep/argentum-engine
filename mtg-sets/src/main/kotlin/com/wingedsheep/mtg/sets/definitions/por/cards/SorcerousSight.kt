// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.LookAtTargetHandEffect
import com.wingedsheep.sdk.scripting.targets.TargetOpponent


/**
 * Sorcerous Sight
 * {U}
 * Sorcery
 * Look at target opponent's hand.
 * Draw a card.
 */
val SorcerousSight = card("Sorcerous Sight") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetOpponent())
        effect = CompositeEffect(
        listOf(
            LookAtTargetHandEffect(t),
            DrawCardsEffect(1)
        )
    )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Kaja Foglio"
        flavorText = "Do not react; anticipate."
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ecfd43dc-e5fd-43bc-babb-fe7ecb6ccd00.jpg"
    }
}
