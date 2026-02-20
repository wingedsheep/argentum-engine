package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect

/**
 * Catapult Master
 * {3}{W}{W}
 * Creature — Human Soldier
 * 3/3
 * Tap five untapped Soldiers you control: Exile target creature.
 */
val CatapultMaster = card("Catapult Master") {
    manaCost = "{3}{W}{W}"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "Tap five untapped Soldiers you control: Exile target creature."

    activatedAbility {
        cost = Costs.TapPermanents(5, GameObjectFilter.Creature.withSubtype("Soldier"))
        target = Targets.Creature
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.EXILE)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "10"
        artist = "Terese Nielsen"
        flavorText = "There's no 'I' in 'team,' but there's a 'we' in 'weapon.'"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a74d7aa2-c6ff-432d-b671-cef58c6736c6.jpg?1562929435"
    }
}
