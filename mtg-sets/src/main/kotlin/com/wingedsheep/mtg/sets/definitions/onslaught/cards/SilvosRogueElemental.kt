package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect

/**
 * Silvos, Rogue Elemental
 * {3}{G}{G}{G}
 * Legendary Creature — Elemental
 * 8/5
 * Trample
 * {G}: Regenerate Silvos.
 */
val SilvosRogueElemental = card("Silvos, Rogue Elemental") {
    manaCost = "{3}{G}{G}{G}"
    typeLine = "Legendary Creature — Elemental"
    power = 8
    toughness = 5
    oracleText = "Trample\n{G}: Regenerate Silvos."

    keywords(Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.Mana("{G}")
        effect = RegenerateEffect(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "282"
        artist = "Carl Critchlow"
        flavorText = "He was born of the Mirari, thrust out of his homeland before he was even aware. Left without purpose or meaning, he found both in the pits."
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3e48715c-6ff7-4b0c-aa7e-a2c901215426.jpg?1562909620"
    }
}
