package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.TargetChooser

/**
 * Goblin Test Pilot
 * {1}{U}{R}
 * Creature — Goblin Pilot Wizard
 * 0/2
 * Flying
 * {T}: This creature deals 2 damage to any target chosen at random.
 *
 * The target is drawn at random as the ability is activated, before {T} is paid, from every legal
 * target — its controller and its own creatures included, hexproof and protected objects excluded.
 *
 * Canonical printing: Dragon's Maze, the card's only printing.
 */
val GoblinTestPilot = card("Goblin Test Pilot") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Goblin Pilot Wizard"
    power = 0
    toughness = 2
    oracleText = "Flying\n{T}: This creature deals 2 damage to any target chosen at random."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Tap
        val anyone = target(AnyTarget(chooser = TargetChooser.Random))
        effect = Effects.DealDamage(2, anyone)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Svetlin Velinov"
        flavorText = "\"All pilots and prototypes destroyed. Extensive collateral damage inflicted. Conclusion: flawless design.\"\n—*Manual of Melek*"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8dbb9aa-1bf8-447d-a96c-33e2248bfb01.jpg"
    }
}
