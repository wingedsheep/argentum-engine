package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mistform Dreamer
 * {2}{U}
 * Creature — Illusion
 * 2/1
 * Flying
 * {1}: Mistform Dreamer becomes the creature type of your choice until end of turn.
 */
val MistformDreamer = card("Mistform Dreamer") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Illusion"
    power = 2
    toughness = 1
    oracleText = "Flying\n{1}: Mistform Dreamer becomes the creature type of your choice until end of turn."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = BecomeCreatureTypeEffect(
            target = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Matthew Mitchell"
        flavorText = "\"Devotion, the second myth of reality: The faithful are most hurt by the objects of their faith.\""
        imageUri = "https://cards.scryfall.io/large/front/f/f/ff34e303-c94a-4f5f-b9f6-8d48e6aac383.jpg?1562955425"
    }
}
