package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Skirk Volcanist
 * {3}{R}
 * Creature — Goblin
 * 3/1
 * Morph—Sacrifice two Mountains.
 * When Skirk Volcanist is turned face up, it deals 3 damage divided as you choose
 * among one, two, or three target creatures.
 */
val SkirkVolcanist = card("Skirk Volcanist") {
    manaCost = "{3}{R}"
    typeLine = "Creature — Goblin"
    power = 3
    toughness = 1
    oracleText = "Morph—Sacrifice two Mountains. (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)\nWhen Skirk Volcanist is turned face up, it deals 3 damage divided as you choose among one, two, or three target creatures."

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        target = TargetCreature(count = 3, minCount = 1)
        effect = DividedDamageEffect(
            totalDamage = 3,
            minTargets = 1,
            maxTargets = 3
        )
    }

    morphCost = PayCost.Sacrifice(GameObjectFilter.Land.withSubtype("Mountain"), count = 2)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "Matt Cavotta"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8cdfb7e3-e077-400a-868d-3f3811e7a35c.jpg?1562532053"
    }
}
