package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Vivid Revival
 * {4}{G}
 * Sorcery
 * Return up to three target multicolored cards from your graveyard to your hand. Exile Vivid Revival.
 */
val VividRevival = card("Vivid Revival") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Return up to three target multicolored cards from your graveyard to your hand. Exile Vivid Revival."

    spell {
        target = TargetObject(
            count = 3,
            minCount = 0,
            filter = TargetFilter(GameObjectFilter.Multicolored.ownedByYou(), zone = Zone.GRAVEYARD)
        )
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND)
        )
        selfExile()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "148"
        artist = "Yeong-Hao Han"
        flavorText = "Golgari crops bloom in strange soils, touched only by long-traveled light."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e67fcfd-0e9e-4b88-8c65-2125fac10a3d.jpg?1783934144"
    }
}
