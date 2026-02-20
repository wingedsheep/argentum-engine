package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.FlipCoinEffect
import com.wingedsheep.sdk.scripting.effects.TurnFaceDownEffect

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
        imageUri = "https://cards.scryfall.io/large/front/4/c/4cc8a6e6-ed62-4784-ba9a-b1f703fc6119.jpg?1562912967"
    }
}
