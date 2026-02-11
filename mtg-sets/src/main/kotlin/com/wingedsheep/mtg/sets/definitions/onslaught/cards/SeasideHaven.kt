package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.DrawCardsEffect
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
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4483b88-6be0-4e7f-9b3b-8b79d0979940.jpg?1562537745"
    }
}
