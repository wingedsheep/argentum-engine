package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brotherhood's End
 * {1}{R}{R}
 * Sorcery
 * Choose one —
 * • Brotherhood's End deals 3 damage to each creature and each planeswalker.
 * • Destroy all artifacts with mana value 3 or less.
 *
 * The sweep mode iterates the combined [GameObjectFilter.CreatureOrPlaneswalker] group, as
 * Dragonback Assault does; the artifact mode is Meltdown's [Effects.DestroyAll] with a fixed cap.
 */
val BrotherhoodsEnd = card("Brotherhood's End") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Brotherhood's End deals 3 damage to each creature and each planeswalker.\n" +
        "• Destroy all artifacts with mana value 3 or less."

    spell {
        modal(chooseCount = 1) {
            mode("Deal 3 damage to each creature and each planeswalker") {
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.CreatureOrPlaneswalker),
                    Effects.DealDamage(3, EffectTarget.IterationEntity),
                )
            }
            mode("Destroy all artifacts with mana value 3 or less") {
                effect = Effects.DestroyAll(GameObjectFilter.Artifact.manaValueAtMost(3))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "Bryan Sola"
        flavorText = "The brothers' rivalry escalated into all-out enmity, and Tocasia paid the price."
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50f7666d-0d60-4fe5-b144-286d4e47b704.jpg?1783920075"
    }
}
