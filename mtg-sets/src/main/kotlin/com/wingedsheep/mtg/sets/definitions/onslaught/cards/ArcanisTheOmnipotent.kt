package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.DrawCardsEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
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
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7a7941b-68bf-48b1-a5ed-2013068b486c.jpg?1562942735"
    }
}
