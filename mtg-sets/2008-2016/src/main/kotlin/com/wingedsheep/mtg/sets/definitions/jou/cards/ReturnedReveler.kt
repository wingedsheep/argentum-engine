package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Returned Reveler
 * {1}{B}
 * Creature — Zombie Satyr
 * 1/3
 * When this creature dies, each player mills three cards.
 */
val ReturnedReveler = card("Returned Reveler") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Satyr"
    power = 1
    toughness = 3
    oracleText = "When this creature dies, each player mills three cards."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Patterns.Library.mill(3, EffectTarget.PlayerRef(Player.Each))
        description = "When this creature dies, each player mills three cards."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "79"
        artist = "Allen Williams"
        flavorText = "The flesh is dead and the life forgotten, but old habits persist."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9ea61098-2c6a-48d2-b99f-f3065c67d8e3.jpg?1783939432"
    }
}
