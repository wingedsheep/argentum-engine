package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Psionic Entity
 * {4}{U}
 * Creature — Illusion
 * 2/2
 * {T}: This creature deals 2 damage to any target and 3 damage to itself.
 */
val PsionicEntity = card("Psionic Entity") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Illusion"
    oracleText = "{T}: This creature deals 2 damage to any target and 3 damage to itself."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Tap
        val target = target(Targets.Any)
        effect = Effects.DealDamage(2, target) then Effects.DealDamage(3, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "67"
        artist = "Justin Hampton"
        flavorText = "Creatures of the Æther are notorious for neglecting their own well-being."
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec082062-5394-4340-bc29-0efd2af4b822.jpg?1783948074"
    }
}
