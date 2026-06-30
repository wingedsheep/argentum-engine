package com.wingedsheep.mtg.sets.definitions.mbs.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Slagstorm
 * {1}{R}{R}
 * Sorcery
 *
 * Choose one —
 * • Slagstorm deals 3 damage to each creature.
 * • Slagstorm deals 3 damage to each player.
 *
 * Canonical printing: Mirrodin Besieged (MBS) — the earliest real expansion printing.
 * The Foundations (FDN) reprint contributes only a `Printing` row.
 */
val Slagstorm = card("Slagstorm") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Slagstorm deals 3 damage to each creature.\n" +
        "• Slagstorm deals 3 damage to each player."

    spell {
        modal(chooseCount = 1) {
            mode("Slagstorm deals 3 damage to each creature") {
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature),
                    DealDamageEffect(3, EffectTarget.Self)
                )
            }
            mode("Slagstorm deals 3 damage to each player") {
                effect = Effects.ForEachPlayer(
                    Player.Each,
                    listOf(DealDamageEffect(3, EffectTarget.Controller))
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "75"
        artist = "Dan Murayama Scott"
        flavorText = "\"As long as we have the will to fight, we are never without weapons.\"\n—Koth of the Hammer"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e318b03-2aad-462b-a2a9-8b6bdf0e93d6.jpg?1782715224"
    }
}
