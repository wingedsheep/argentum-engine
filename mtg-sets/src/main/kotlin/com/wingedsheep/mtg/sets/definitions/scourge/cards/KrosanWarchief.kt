package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceSpellCostBySubtype
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Krosan Warchief
 * {2}{G}
 * Creature — Beast
 * 2/2
 * Beast spells you cast cost {1} less to cast.
 * {1}{G}: Regenerate target Beast.
 */
val KrosanWarchief = card("Krosan Warchief") {
    manaCost = "{2}{G}"
    typeLine = "Creature — Beast"
    power = 2
    toughness = 2
    oracleText = "Beast spells you cast cost {1} less to cast.\n{1}{G}: Regenerate target Beast."

    staticAbility {
        ability = ReduceSpellCostBySubtype(
            subtype = "Beast",
            amount = 1
        )
    }

    activatedAbility {
        cost = Costs.Mana("{1}{G}")
        val t = target("target", TargetPermanent(
            filter = TargetFilter(GameObjectFilter.Creature.withSubtype("Beast"))
        ))
        effect = RegenerateEffect(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Greg Hildebrandt"
        flavorText = "\"It turns prey into predator.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/3/435b700b-2072-47c0-9725-ad04414d2474.jpg?1562528085"
    }
}
