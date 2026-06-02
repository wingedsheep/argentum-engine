package com.wingedsheep.mtg.sets.definitions.mir.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.dsl.Effects

/**
 * Blinding Light
 * {2}{W}
 * Sorcery
 * Tap all nonwhite creatures.
 *
 * Canonical printing: Mirage (1996), the earliest real expansion. Later printings
 * (Portal, Starter 1999, Invasion) add `Printing` rows in their own packages.
 */
val BlindingLight = card("Blinding Light") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Tap all nonwhite creatures."

    spell {
        effect = Effects.ForEachInGroup(GroupFilter.AllCreatures.notColor(Color.WHITE), TapUntapEffect(EffectTarget.Self, tap = true))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "5"
        artist = "Hannibal King"
        flavorText = "\"My shield will bear a shining sun so you will always be with me. / Inlaid with gold, it will shine like glowing embers.\"\n—\"Love Song of Night and Day\""
        imageUri = "https://cards.scryfall.io/normal/front/9/9/99b2192a-78a5-4579-94ce-cccf773a809d.jpg?1587912357"
    }
}
