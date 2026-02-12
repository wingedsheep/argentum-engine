package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GainLifeEffect

/**
 * Ravenous Baloth
 * {2}{G}{G}
 * Creature — Beast
 * 4/4
 * Sacrifice a Beast: You gain 4 life.
 */
val RavenousBaloth = card("Ravenous Baloth") {
    manaCost = "{2}{G}{G}"
    typeLine = "Creature — Beast"
    power = 4
    toughness = 4
    oracleText = "Sacrifice a Beast: You gain 4 life."

    activatedAbility {
        cost = AbilityCost.Sacrifice(GameObjectFilter.Creature.withSubtype("Beast"))
        effect = GainLifeEffect(4)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "278"
        artist = "Arnie Swekel"
        flavorText = "\"All we know about the Krosan Forest we have learned from those few who made it out alive.\"\n—Elvish refugee"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c98182d6-5b25-4493-9286-f29633e1bec4.jpg?1592666556"
    }
}
