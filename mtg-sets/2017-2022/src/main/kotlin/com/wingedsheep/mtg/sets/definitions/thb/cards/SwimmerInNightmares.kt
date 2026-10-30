package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Swimmer in Nightmares
 * {2}{U}
 * Creature — Nightmare Merfolk
 * 1/4
 * This creature gets +3/+0 as long as there are ten or more cards in a single graveyard.
 * This creature can't be blocked as long as you control an Ashiok planeswalker.
 *
 * "A single graveyard" is the greatest graveyard size measured once per player
 * ([DynamicAmounts.greatestAmongPlayers] over `cardsInYourGraveyard`, rebound to each player), so
 * two graveyards of five don't add up and several graveyards of ten still give just +3/+0
 * (ruling). The evasion is Tome Anima's conditional [CantBeBlocked] gated on an Ashiok
 * planeswalker (Arisen Gorgon's subtype gate).
 */
val SwimmerInNightmares = card("Swimmer in Nightmares") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Nightmare Merfolk"
    power = 1
    toughness = 4
    oracleText = "This creature gets +3/+0 as long as there are ten or more cards in a single graveyard.\n" +
        "This creature can't be blocked as long as you control an Ashiok planeswalker."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(3, 0, GroupFilter.source()),
            condition = Conditions.CompareAmounts(
                DynamicAmounts.greatestAmongPlayers(DynamicAmounts.cardsInYourGraveyard()),
                ComparisonOperator.GTE,
                10,
            ),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CantBeBlocked(),
            condition = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Ashiok")),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "275"
        artist = "Lucas Graciano"
        flavorText = "Ashiok's minions plumb the depths of the ocean and the psyche."
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebc6b5ee-e764-444e-8b27-26e00f4b2074.jpg?1783931497"
        ruling(
            "2020-01-24",
            "The first ability of Swimmer in Nightmares gives it just +3/+0, no matter how many graveyards " +
                "beyond the first have ten cards."
        )
    }
}
