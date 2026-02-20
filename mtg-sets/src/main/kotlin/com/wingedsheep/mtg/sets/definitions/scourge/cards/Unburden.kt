package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Unburden
 * {1}{B}{B}
 * Sorcery
 * Target player discards two cards.
 * Cycling {2}
 */
val Unburden = card("Unburden") {
    manaCost = "{1}{B}{B}"
    typeLine = "Sorcery"
    oracleText = "Target player discards two cards.\nCycling {2}"

    spell {
        target = TargetPlayer()
        effect = Effects.Discard(2, EffectTarget.ContextTarget(0))
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Wayne England"
        flavorText = "Cabal initiates enter training full of hopes and fears. They graduate with neither."
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd5fc0e0-4ee5-40eb-a9f0-9b1fff2adefc.jpg?1562533810"
    }
}
