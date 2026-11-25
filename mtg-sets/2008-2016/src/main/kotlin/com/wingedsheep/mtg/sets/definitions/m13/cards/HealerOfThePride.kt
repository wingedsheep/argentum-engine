package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Healer of the Pride
 * {3}{W}
 * Creature — Cat Cleric
 * 2/3
 * Whenever another creature you control enters, you gain 2 life.
 */
val HealerOfThePride = card("Healer of the Pride") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Cleric"
    oracleText = "Whenever another creature you control enters, you gain 2 life."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        effect = Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "19"
        artist = "Christopher Moeller"
        flavorText = "\"No life is without meaning. No living thing is too small to be strong.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35716e37-1bb2-41e2-bb55-e65126b01ce3.jpg"
    }
}
