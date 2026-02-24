package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.core.Zone

/**
 * Arcanis the Omnipotent
 * {3}{U}{U}{U}
 * Legendary Creature — Wizard
 * 3/4
 * {T}: Draw three cards.
 * {2}{U}{U}: Return Arcanis the Omnipotent to its owner's hand.
 */
val ArcanisTheOmnipotent = card("Arcanis the Omnipotent") {
    manaCost = "{3}{U}{U}{U}"
    typeLine = "Legendary Creature — Wizard"
    power = 3
    toughness = 4
    oracleText = "{T}: Draw three cards.\n{2}{U}{U}: Return Arcanis the Omnipotent to its owner's hand."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = DrawCardsEffect(3)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{U}{U}")
        effect = MoveToZoneEffect(EffectTarget.Self, Zone.HAND)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Justin Sweet"
        flavorText = "He has journeyed where none have been before. Now he returns to ensure that none follow."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90865f52-c062-4505-a204-b4d7d4b3fc4c.jpg?1562929057"
    }
}
