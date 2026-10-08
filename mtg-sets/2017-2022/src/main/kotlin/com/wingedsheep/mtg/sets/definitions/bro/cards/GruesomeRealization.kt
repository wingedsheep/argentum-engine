package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gruesome Realization
 * {1}{B}{B}
 * Sorcery
 * Choose one —
 * • You draw two cards and you lose 2 life.
 * • Creatures your opponents control get -1/-1 until end of turn.
 */
val GruesomeRealization = card("Gruesome Realization") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• You draw two cards and you lose 2 life.\n" +
        "• Creatures your opponents control get -1/-1 until end of turn."

    spell {
        modal(chooseCount = 1) {
            mode("You draw two cards and you lose 2 life") {
                effect = Effects.DrawCards(2) then Effects.LoseLife(2, EffectTarget.Controller)
            }
            mode("Creatures your opponents control get -1/-1 until end of turn") {
                effect = Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.opponentControls()),
                    Effects.ModifyStats(-1, -1, EffectTarget.IterationEntity)
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "103"
        artist = "Julie Dillon"
        flavorText = "Mishra was gone—all that remained was a machine wearing his face."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/21cd0ece-a267-42ab-b95a-6e7931bd837a.jpg"
    }
}
