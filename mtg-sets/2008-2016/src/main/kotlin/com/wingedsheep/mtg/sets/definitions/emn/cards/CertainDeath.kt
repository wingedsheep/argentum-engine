package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Certain Death
 * {5}{B}
 * Sorcery
 * Destroy target creature. Its controller loses 2 life and you gain 2 life.
 */
val CertainDeath = card("Certain Death") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature. Its controller loses 2 life and you gain 2 life."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Destroy(creature) then
            Effects.LoseLife(2, EffectTarget.TargetController) then
            Effects.GainLife(2, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Kev Walker"
        flavorText = "Some spirits cling to one person, feasting on the growing fear until a final moment of all-consuming terror."
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c67784b3-eb55-452e-b965-f63220b88896.jpg?1783937489"
        ruling(
            "2016-07-13",
            "If the creature becomes an illegal target for Certain Death, no player gains or loses life. " +
                "If it's a legal target but isn't destroyed, most likely due to having indestructible, " +
                "its controller still loses 2 life and you gain 2 life."
        )
    }
}
