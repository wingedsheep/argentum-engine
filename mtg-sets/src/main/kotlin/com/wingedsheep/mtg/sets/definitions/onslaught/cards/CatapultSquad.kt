package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TargetFilter
import com.wingedsheep.sdk.targeting.TargetPermanent

/**
 * Catapult Squad
 * {1}{W}
 * Creature — Human Soldier
 * 2/1
 * Tap two untapped Soldiers you control: Catapult Squad deals 2 damage to target attacking or blocking creature.
 */
val CatapultSquad = card("Catapult Squad") {
    manaCost = "{1}{W}"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 1
    oracleText = "Tap two untapped Soldiers you control: Catapult Squad deals 2 damage to target attacking or blocking creature."

    activatedAbility {
        cost = AbilityCost.TapPermanents(
            count = 2,
            filter = GameObjectFilter.Creature.withSubtype("Soldier")
        )
        target = TargetPermanent(filter = TargetFilter.AttackingOrBlockingCreature)
        effect = DealDamageEffect(2, EffectTarget.ContextTarget(0))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "11"
        artist = "Brian Snõddy"
        flavorText = "Together they could hit anything between the heavens and the horizon."
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75a71d29-29eb-43c4-b0f3-457435e8f629.jpg?1562922810"
    }
}
