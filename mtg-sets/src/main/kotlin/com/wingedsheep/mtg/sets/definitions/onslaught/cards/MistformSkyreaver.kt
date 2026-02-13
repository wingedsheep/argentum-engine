package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.EffectTarget

/**
 * Mistform Skyreaver
 * {5}{U}{U}
 * Creature — Illusion
 * 6/6
 * Flying
 * {1}: Mistform Skyreaver becomes the creature type of your choice until end of turn.
 */
val MistformSkyreaver = card("Mistform Skyreaver") {
    manaCost = "{5}{U}{U}"
    typeLine = "Creature — Illusion"
    power = 6
    toughness = 6
    oracleText = "Flying\n{1}: Mistform Skyreaver becomes the creature type of your choice until end of turn."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = BecomeCreatureTypeEffect(
            target = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "97"
        artist = "Anthony S. Waters"
        flavorText = "\"Conviction, the third myth of reality: Only those who seek the truth can be deceived.\""
        imageUri = "https://cards.scryfall.io/large/front/e/3/e394e096-ea70-4813-9039-e4bd065d0a17.jpg"
    }
}
