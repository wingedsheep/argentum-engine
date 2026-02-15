package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.FlipCoinEffect
import com.wingedsheep.sdk.scripting.TurnFaceDownEffect

/**
 * Skittish Valesk
 * {6}{R}
 * Creature — Beast
 * 5/5
 * At the beginning of your upkeep, flip a coin. If you lose the flip, turn
 * Skittish Valesk face down.
 * Morph {5}{R}
 */
val SkittishValesk = card("Skittish Valesk") {
    manaCost = "{6}{R}"
    typeLine = "Creature — Beast"
    power = 5
    toughness = 5
    oracleText = "At the beginning of your upkeep, flip a coin. If you lose the flip, turn Skittish Valesk face down.\nMorph {5}{R}"

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        effect = FlipCoinEffect(
            lostEffect = TurnFaceDownEffect(EffectTarget.Self)
        )
    }

    morph = "{5}{R}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Alan Pollack"
        flavorText = "Skirk Ridge goblins have learned to take cover when they hear its roar."
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10ad7bde-3e87-4357-9849-20c727e4aa53.jpg?1562898198"
    }
}
