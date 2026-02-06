package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.LoseLifeEffect
import com.wingedsheep.sdk.scripting.OnOtherCreatureEnters

/**
 * Wretched Anurid
 * {1}{B}
 * Creature — Zombie Frog Beast
 * 3/3
 * Whenever a creature enters the battlefield, you lose 1 life.
 */
val WretchedAnurid = card("Wretched Anurid") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Zombie Frog Beast"
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = OnOtherCreatureEnters(youControlOnly = false)
        effect = LoseLifeEffect(1, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "183"
        artist = "Glen Angus"
        flavorText = "The only prince inside this frog is the one it ate."
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aab525ad-1f62-4d9c-9b74-c7b0048da452.jpg?1562935315"
    }
}
