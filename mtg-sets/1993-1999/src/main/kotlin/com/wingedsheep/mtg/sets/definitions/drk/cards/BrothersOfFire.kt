package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brothers of Fire
 * {1}{R}{R}
 * Creature — Human Shaman
 * 2/2
 * {1}{R}{R}: This creature deals 1 damage to any target and 1 damage to you.
 */
val BrothersOfFire = card("Brothers of Fire") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    oracleText = "{1}{R}{R}: This creature deals 1 damage to any target and 1 damage to you."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{1}{R}{R}")
        val target = target("target", AnyTarget())
        effect = Effects.DealDamage(1, target)
            .then(Effects.DealDamage(1, EffectTarget.PlayerRef(Player.You)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Mark Tedin"
        flavorText = "Fire is never a gentle master."
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba2cc4a6-fdcc-4082-801a-d2c50e560e8d.jpg?1783947936"
    }
}
