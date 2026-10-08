package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Fog of War
 * {2}{G}
 * Instant
 * You gain 1 life for each creature on the battlefield. Prevent all combat damage that would be
 * dealt this turn by creatures with power 3 or less.
 *
 * The life gain counts every creature, whoever controls it (`DynamicAmounts.allCreatures()`, as on
 * Blunt the Assault). The prevention is a source-side shield like Vine Snare's: power is read each
 * time damage would be dealt, so a creature pumped above 3 after this resolves deals its damage.
 */
val FogOfWar = card("Fog of War") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "You gain 1 life for each creature on the battlefield. Prevent all combat damage that would be dealt this turn by creatures with power 3 or less."

    spell {
        effect = Effects.GainLife(DynamicAmounts.allCreatures()) then
            Effects.PreventCombatDamageFrom(GameObjectFilter.Creature.powerAtMost(3))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "180"
        artist = "Josu Hernaiz"
        flavorText = "So thick was the fog on Argoth's shores that the brothers' forces passed within shouting distance of each other without realizing it."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37096c19-32c9-448a-94e9-f5fe2d74e3a3.jpg"
    }
}
