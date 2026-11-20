package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fleshmad Steed
 * {1}{B}
 * Creature — Horse
 * 2/2
 * Whenever another creature dies, tap this creature.
 */
val FleshmadSteed = card("Fleshmad Steed") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Horse"
    power = 2
    toughness = 2
    oracleText = "Whenever another creature dies, tap this creature."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature).dies()
        effect = Effects.Tap(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "88"
        artist = "Robbie Trevino"
        flavorText = "More disturbing than the unknown is a distortion of the familiar."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/378c5ba0-bf4f-43d5-b371-0991db5a2e32.jpg?1783939780"
    }
}
