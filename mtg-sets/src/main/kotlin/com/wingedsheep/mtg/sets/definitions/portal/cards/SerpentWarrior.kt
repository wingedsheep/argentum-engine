package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.LoseLifeEffect
import com.wingedsheep.sdk.scripting.OnEnterBattlefield

/**
 * Serpent Warrior
 * {2}{B}
 * Creature — Snake Warrior
 * 3/3
 * When Serpent Warrior enters the battlefield, you lose 3 life.
 */
val SerpentWarrior = card("Serpent Warrior") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Snake Warrior"
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = OnEnterBattlefield()
        effect = LoseLifeEffect(3, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "109"
        artist = "Andrew Robinson"
        flavorText = "Its venom courses through your veins even as you command it."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c364fd06-64c5-45f6-8ed5-64f44a1e8bda.jpg"
    }
}
