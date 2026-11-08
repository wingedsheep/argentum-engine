package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ingenious Skaab
 * {2}{U}
 * Creature — Zombie Horror
 * 2/3
 *
 * Prowess
 * {U}: This creature gets +1/-1 until end of turn.
 */
val IngeniousSkaab = card("Ingenious Skaab") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Zombie Horror"
    power = 2
    toughness = 3
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\n" +
        "{U}: This creature gets +1/-1 until end of turn."

    prowess()

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.ModifyStats(1, -1, EffectTarget.Self)
        description = "{U}: This creature gets +1/-1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Jesper Ejsing"
        flavorText = "Heads and shoulders above the rest."
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19d6ed0b-40ba-407d-94de-05a705e79b95.jpg?1783937497"
    }
}
