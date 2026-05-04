package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Shattered Wings
 * {2}{G}
 * Sorcery
 * Destroy target artifact, enchantment, or creature with flying. Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)
 */
val ShatteredWings = card("Shattered Wings") {
    manaCost = "{2}{G}"
    typeLine = "Sorcery"
    oracleText = "Destroy target artifact, enchantment, or creature with flying. Surveil 1. (Look at the top card of your library. You may put it into your graveyard.)"

    spell {
        val target = target("target artifact, enchantment, or creature with flying", TargetPermanent(filter = TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.Enchantment or GameObjectFilter.Creature.withKeyword(Keyword.FLYING))))
        effect = Effects.Composite(
            listOf(
                Effects.Destroy(target),
                EffectPatterns.surveil(1)
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "206"
        artist = "Sergey Glushakov"
        flavorText = "The Eumidians sought to keep Evendo a gentle world—at any cost."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbece737-bd4f-4dc8-bf5c-f77930246ab1.jpg?1752947394"
    }
}
