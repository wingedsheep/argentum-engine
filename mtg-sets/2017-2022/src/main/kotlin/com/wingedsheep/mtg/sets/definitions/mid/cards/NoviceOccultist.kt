package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Novice Occultist
 * {1}{B}
 * Creature — Human Wizard
 * 1/2
 * When this creature dies, you draw a card and you lose 1 life.
 */
val NoviceOccultist = card("Novice Occultist") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Wizard"
    oracleText = "When this creature dies, you draw a card and you lose 1 life."
    power = 1
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.DrawCards(1) then Effects.LoseLife(1, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Zara Alfonso"
        flavorText = "All across Gavony, in shuttered bedrooms and locked barns, young people seek the answers their elders failed to provide."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a541ac79-a969-42b4-b989-f6e37572ec90.jpg"
    }
}
