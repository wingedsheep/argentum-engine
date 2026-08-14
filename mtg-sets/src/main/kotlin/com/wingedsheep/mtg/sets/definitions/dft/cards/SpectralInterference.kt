package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetSpell

/**
 * Spectral Interference — Aetherdrift #63
 * {1}{U} · Instant
 *
 * Counter target artifact or creature spell unless its controller pays {4}.
 *
 * Same soft-counter shape as [SpellPierce], narrowed to the artifact/creature half of the stack
 * ([GameObjectFilter.CreatureOrArtifact] restricted to [Zone.STACK] — an artifact creature spell
 * matches on either half). The tax is a fixed {4}, so [Effects.CounterUnlessPays] rather than the
 * dynamic variant.
 */
val SpectralInterference = card("Spectral Interference") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target artifact or creature spell unless its controller pays {4}."

    spell {
        target = TargetSpell(
            filter = TargetFilter(GameObjectFilter.CreatureOrArtifact, zone = Zone.STACK)
        )
        effect = Effects.CounterUnlessPays("{4}")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "Steve Ellis"
        flavorText = "\"So many fears. Let's make them all come true.\""
        imageUri = "https://cards.scryfall.io/normal/front/8/6/860cb8af-a5f6-47e7-a34b-7b9f11ddc8c6.jpg?1783907903"
    }
}
