// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Symbol of Unsummoning
 * {2}{U}
 * Sorcery
 * Return target creature to its owner's hand.
 * Draw a card.
 */
val SymbolofUnsummoning = card("Symbol of Unsummoning") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = CompositeEffect(
        listOf(
            MoveToZoneEffect(t, Zone.HAND),
            DrawCardsEffect(1)
        )
    )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Adam Rex"
        flavorText = "\". . . inviting the soul to wander for a spell in abysses of solitude . . . .\"\n—Kate Chopin, The Awakening"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55811106-9f30-4e34-924e-2c9401b49574.jpg"
    }
}
