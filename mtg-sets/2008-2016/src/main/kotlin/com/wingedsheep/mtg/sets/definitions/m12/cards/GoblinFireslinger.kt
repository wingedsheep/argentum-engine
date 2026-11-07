package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Goblin Fireslinger
 * {R}
 * Creature — Goblin Warrior
 * 1/1
 *
 * {T}: This creature deals 1 damage to target player or planeswalker.
 */
val GoblinFireslinger = card("Goblin Fireslinger") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    power = 1
    toughness = 1
    oracleText = "{T}: This creature deals 1 damage to target player or planeswalker."

    activatedAbility {
        cost = Costs.Tap
        val victim = target(Targets.PlayerOrPlaneswalker)
        effect = Effects.DealDamage(1, victim)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Pete Venters"
        flavorText = "A rock between your eyes hurts. A burning rock between your eyes ruins your whole day."
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c11db78-f506-4af2-a7be-c7ac2c0ffcf3.jpg?1783941070"
    }
}
