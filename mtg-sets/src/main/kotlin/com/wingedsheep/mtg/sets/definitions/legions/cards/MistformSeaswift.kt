package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mistform Seaswift
 * {3}{U}
 * Creature — Illusion
 * 3/1
 * Flying
 * {1}: Mistform Seaswift becomes the creature type of your choice until end of turn.
 * Morph {1}{U}
 */
val MistformSeaswift = card("Mistform Seaswift") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Illusion"
    power = 3
    toughness = 1
    oracleText = "Flying\n{1}: This creature becomes the creature type of your choice until end of turn.\nMorph {1}{U} (You may cast this card face down as a 2/2 creature for {3}. Turn it face up any time for its morph cost.)"

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = BecomeCreatureTypeEffect(
            target = EffectTarget.Self
        )
    }

    morph = "{1}{U}"

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "45"
        artist = "Dany Orizio"
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2f6c73c-8162-499f-8d16-92f17c0c2bee.jpg?1562931016"
    }
}
