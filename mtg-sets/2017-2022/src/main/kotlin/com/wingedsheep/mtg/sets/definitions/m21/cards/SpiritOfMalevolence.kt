package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spirit of Malevolence
 * {1}{B}
 * Creature — Spirit
 * 2/1
 * When this creature dies, each opponent loses 1 life and you gain 1 life.
 */
val SpiritOfMalevolence = card("Spirit of Malevolence") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 1
    oracleText = "When this creature dies, each opponent loses 1 life and you gain 1 life."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "331"
        artist = "Josu Hernaiz"
        flavorText = "Great evil weighs on the soul."
        imageUri = "https://cards.scryfall.io/normal/front/c/f/cf3765b7-11e2-4837-b5e5-ca24adc0e33c.jpg?1783930618"
        ruling("2020-06-23", "If your life total is brought to 0 or less at the same time that Spirit of Malevolence is dealt lethal damage, you lose the game before its ability goes on the stack.")
        ruling("2020-06-23", "In a Two-Headed Giant game, Spirit of Malevolence's ability causes the opposing team to lose 2 life and you to gain 1 life.")
    }
}
