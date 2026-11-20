package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deathbellow Raider
 * {1}{R}
 * Creature — Minotaur Berserker
 * 2/3
 *
 * This creature attacks each combat if able.
 * {2}{B}: Regenerate this creature.
 */
val DeathbellowRaider = card("Deathbellow Raider") {
    manaCost = "{1}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Minotaur Berserker"
    power = 2
    toughness = 3
    oracleText = "This creature attacks each combat if able.\n" +
        "{2}{B}: Regenerate this creature."

    staticAbility {
        ability = MustAttack()
    }

    activatedAbility {
        cost = Costs.Mana("{2}{B}")
        effect = Effects.Regenerate(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "117"
        artist = "Wayne Reynolds"
        flavorText = "\"The temple has been rededicated. It belongs to Mogis now.\"\n—Rastos, disciple of Mogis"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b29fc64e-297e-4e10-8f4e-289dc1c3ebfd.jpg?1783939765"
    }
}
