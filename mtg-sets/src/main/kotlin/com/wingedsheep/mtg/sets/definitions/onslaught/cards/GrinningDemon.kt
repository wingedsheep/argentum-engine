package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect

/**
 * Grinning Demon
 * {2}{B}{B}
 * Creature — Demon
 * 6/6
 * At the beginning of your upkeep, you lose 2 life.
 * Morph {2}{B}{B}
 */
val GrinningDemon = card("Grinning Demon") {
    manaCost = "{2}{B}{B}"
    typeLine = "Creature — Demon"
    power = 6
    toughness = 6
    oracleText = "At the beginning of your upkeep, you lose 2 life.\nMorph {2}{B}{B}"

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = LoseLifeEffect(2, EffectTarget.Controller)
    }

    morph = "{2}{B}{B}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Mark Zug"
        flavorText = "It's drawn to the scent of screaming."
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72de2f66-0b86-4c21-b4c8-c2d97e3fd095.jpg?1562922147"
    }
}
