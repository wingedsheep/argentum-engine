package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Seaside Haven
 * Land
 * {T}: Add {C}.
 * {W}{U}, {T}, Sacrifice a Bird: Draw a card.
 */
val SeasideHaven = card("Seaside Haven") {
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n{W}{U}, {T}, Sacrifice a Bird: Draw a card."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = AddColorlessManaEffect(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{W}{U}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Creature.withSubtype("Bird"))
        )
        effect = DrawCardsEffect(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "323"
        artist = "Mark Brill"
        imageUri = "https://cards.scryfall.io/large/front/9/c/9c940a6b-3c5e-4ce2-92b6-63e2cb575c15.jpg?1562931946"
    }
}
