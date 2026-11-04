package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shake the Foundations
 * {2}{R}
 * Instant
 * Shake the Foundations deals 1 damage to each creature without flying.
 * Draw a card.
 */
val ShakeTheFoundations = card("Shake the Foundations") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Shake the Foundations deals 1 damage to each creature without flying.\nDraw a card."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING)),
            Effects.DealDamage(1, EffectTarget.IterationEntity),
        ) then Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "113"
        artist = "Zack Stella"
        flavorText = "A threefold calamity with a single will."
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5bd4bdd-3a2a-40d9-9f86-fefe0a462cd2.jpg?1783935296"
    }
}
