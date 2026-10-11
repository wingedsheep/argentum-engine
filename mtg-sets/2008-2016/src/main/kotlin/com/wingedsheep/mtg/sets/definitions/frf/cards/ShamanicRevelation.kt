package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Shamanic Revelation
 * {3}{G}{G}
 * Sorcery
 *
 * Draw a card for each creature you control.
 * Ferocious — You gain 4 life for each creature you control with power 4 or greater.
 *
 * Both counts are taken as each instruction resolves, in order — the draw doesn't change the
 * board, so the second count sees the same creatures (with their current, projected power).
 */
val ShamanicRevelation = card("Shamanic Revelation") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Draw a card for each creature you control.\nFerocious — You gain 4 life for each creature you control with power 4 or greater."

    spell {
        effect = Effects.DrawCards(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).count()
        ) then Effects.GainLife(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature.powerAtLeast(4)).count() * 4
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "138"
        artist = "Cynthia Sheppard"
        flavorText = "First, whispers of ancient days. Then, visions of things to come."
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c7f2c73-26d8-42a6-acde-2c5a2f4e21a1.jpg?1783938679"
    }
}
