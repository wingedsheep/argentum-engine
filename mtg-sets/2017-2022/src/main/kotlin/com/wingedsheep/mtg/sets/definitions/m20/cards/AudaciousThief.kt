package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Audacious Thief
 * {2}{B}
 * Creature — Human Rogue
 * 2/2
 *
 * Whenever this creature attacks, you draw a card and you lose 1 life.
 */
val AudaciousThief = card("Audacious Thief") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 2
    toughness = 2
    oracleText = "Whenever this creature attacks, you draw a card and you lose 1 life."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
        description = "Whenever this creature attacks, you draw a card and you lose 1 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Dmitry Burmak"
        flavorText = "\"Skill, imagination, smarts: none of it matters unless you have the nerve to use it.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1ff5e66-4718-43c3-8a58-1a0a8e788f83.jpg?1783933000"
    }
}
