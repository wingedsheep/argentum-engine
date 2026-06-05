// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Cloak of Feathers
 * {U}
 * Sorcery
 * Target creature gains flying until end of turn.
 * Draw a card.
 */
val CloakofFeathers = card("Cloak of Feathers") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = CompositeEffect(
        listOf(
            GrantKeywordEffect(Keyword.FLYING, t),
            DrawCardsEffect(1)
        )
    )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "44"
        artist = "Rebecca Guay"
        flavorText = "A thousand feathers from a thousand birds, sewn with magic and song."
        imageUri = "https://cards.scryfall.io/normal/front/9/7/9746790c-a426-4135-8c9d-afb82a0c26b8.jpg"
    }
}
