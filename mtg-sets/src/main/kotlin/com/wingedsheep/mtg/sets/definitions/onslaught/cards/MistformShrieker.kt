package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.EffectTarget

/**
 * Mistform Shrieker
 * {3}{U}{U}
 * Creature — Illusion
 * 3/3
 * Flying
 * {1}: Mistform Shrieker becomes the creature type of your choice until end of turn.
 * Morph {3}{U}{U}
 */
val MistformShrieker = card("Mistform Shrieker") {
    manaCost = "{3}{U}{U}"
    typeLine = "Creature — Illusion"
    power = 3
    toughness = 3

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = BecomeCreatureTypeEffect(
            target = EffectTarget.Self
        )
    }

    morph = "{3}{U}{U}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Glen Angus"
        imageUri = "https://cards.scryfall.io/large/front/1/0/1082eea2-5e83-48d4-b02b-a22e7cbe2054.jpg?1562898958"
    }
}
