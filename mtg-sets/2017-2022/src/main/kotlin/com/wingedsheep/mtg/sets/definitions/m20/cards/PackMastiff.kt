package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Pack Mastiff — Core Set 2020 #152
 * {1}{R} · Creature — Dog · 2/2 · Common
 *
 * {1}{R}: Each creature you control named Pack Mastiff gets +1/+0 until end of turn.
 *
 * Riot Ringleader's [Patterns.Group.modifyStatsForAll] pump, filtered by name rather than subtype.
 * The group is locked in on resolution, so a Pack Mastiff entering afterwards isn't pumped.
 */
val PackMastiff = card("Pack Mastiff") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dog"
    power = 2
    toughness = 2
    oracleText = "{1}{R}: Each creature you control named Pack Mastiff gets +1/+0 until end of turn."

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Patterns.Group.modifyStatsForAll(
            1, 0,
            GroupFilter(GameObjectFilter.Creature.named("Pack Mastiff").youControl())
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "152"
        artist = "Svetlin Velinov"
        flavorText = "Their loyalty is not to any master, but to the pack."
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39934090-36a6-4183-9176-97ea932d2685.jpg?1783932973"
    }
}
