package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Gravel Slinger
 * {3}{W}
 * Creature — Human Soldier
 * 1/3
 * {T}: Gravel Slinger deals 1 damage to target attacking or blocking creature.
 * Morph {1}{W}
 */
val GravelSlinger = card("Gravel Slinger") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 3

    activatedAbility {
        cost = AbilityCost.Tap
        target = TargetPermanent(filter = TargetFilter.AttackingOrBlockingCreature)
        effect = DealDamageEffect(1, EffectTarget.ContextTarget(0))
    }

    morph = "{1}{W}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87551307-6b5f-4f12-aa1f-4beebefad3b3.jpg?1562926972"
    }
}
