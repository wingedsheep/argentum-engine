package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AddCountersEffect
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Carrion Feeder
 * {B}
 * Creature — Zombie
 * 1/1
 * Carrion Feeder can't block.
 * Sacrifice a creature: Put a +1/+1 counter on Carrion Feeder.
 */
val CarrionFeeder = card("Carrion Feeder") {
    manaCost = "{B}"
    typeLine = "Creature — Zombie"
    power = 1
    toughness = 1
    oracleText = "Carrion Feeder can't block.\nSacrifice a creature: Put a +1/+1 counter on Carrion Feeder."

    staticAbility {
        ability = CantBlock()
    }

    activatedAbility {
        cost = AbilityCost.Sacrifice(GameObjectFilter.Creature)
        effect = AddCountersEffect(
            counterType = "+1/+1",
            count = 1,
            target = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "59"
        artist = "Brian Snõddy"
        flavorText = "Stinking of rot, it leaps between gravestones in search of its next meal."
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88042031-64af-4f84-85d5-95992b43aa6c.jpg?1562531649"
    }
}
