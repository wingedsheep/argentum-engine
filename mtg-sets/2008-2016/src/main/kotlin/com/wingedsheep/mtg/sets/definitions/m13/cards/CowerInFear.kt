package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cower in Fear
 * {1}{B}{B}
 * Instant
 * Creatures your opponents control get -1/-1 until end of turn.
 */
val CowerInFear = card("Cower in Fear") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Creatures your opponents control get -1/-1 until end of turn."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesOpponentsControl,
            Effects.ModifyStats(-1, -1, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "84"
        artist = "Nils Hamm"
        flavorText = "\"You will fully understand fear when you discover it is the final thing you put your faith in.\"\n—Nicol Bolas"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf2d53b8-7847-4b94-9711-eca29facccba.jpg"
    }
}
