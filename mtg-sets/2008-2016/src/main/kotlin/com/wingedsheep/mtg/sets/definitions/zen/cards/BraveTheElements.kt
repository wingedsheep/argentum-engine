package com.wingedsheep.mtg.sets.definitions.zen.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brave the Elements
 * {W}
 * Instant
 * Choose a color. White creatures you control gain protection from the chosen color until end of turn.
 */
val BraveTheElements = card("Brave the Elements") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose a color. White creatures you control gain protection from the chosen color until end of turn."

    spell {
        effect = Effects.ChooseColorThen(
            Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.withColor(Color.WHITE).youControl()),
                Effects.GrantProtectionFromChosenColor(EffectTarget.IterationEntity)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "4"
        artist = "Goran Josic"
        flavorText = "\"Trust me, your lost fingers and toes are nothing compared to the lost treasures of Sejiri.\"\n—Zahr Gada, Halimar expedition leader"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c14c492d-3fd7-4b2a-910e-bfcb33752eba.jpg?1783942176"
    }
}
