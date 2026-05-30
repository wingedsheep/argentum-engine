package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glimmering Angel
 * {3}{W}
 * Creature — Angel
 * 2/2
 * Flying
 * {U}: This creature gains shroud until end of turn.
 */
val GlimmeringAngel = card("Glimmering Angel") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 2
    toughness = 2
    oracleText = "Flying\n{U}: This creature gains shroud until end of turn. " +
        "(It can't be the target of spells or abilities.)"

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.GrantKeyword(Keyword.SHROUD, EffectTarget.Self)
        description = "{U}: This creature gains shroud until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Ciruelo"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f14f55e4-eded-4a86-87f4-b8fa6f30bc0f.jpg?1562943497"
    }
}
