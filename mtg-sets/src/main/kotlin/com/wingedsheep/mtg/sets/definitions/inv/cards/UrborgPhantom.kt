package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Urborg Phantom
 * {2}{B}
 * Creature — Spirit Minion
 * 3/1
 * This creature can't block.
 * {U}: Prevent all combat damage that would be dealt to and dealt by this creature this turn.
 */
val UrborgPhantom = card("Urborg Phantom") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Spirit Minion"
    power = 3
    toughness = 1
    oracleText = "This creature can't block.\n{U}: Prevent all combat damage that would be dealt to and dealt by this creature this turn."

    staticAbility {
        ability = CantBlock()
    }

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.PreventCombatDamageToAndBy(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "132"
        artist = "Daren Bader"
        flavorText = "A chilling fog with teeth of ice."
        imageUri = "https://cards.scryfall.io/normal/front/3/9/397355b9-5b67-4973-972e-3505c500d116.jpg?1562906570"
    }
}
