package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ahn-Crop Invader — War of the Spark #113
 * {2}{R} · Creature — Zombie Minotaur Warrior · 2/2
 *
 * During your turn, this creature has first strike.
 * {1}, Sacrifice another creature: This creature gets +2/+0 until end of turn.
 */
val AhnCropInvader = card("Ahn-Crop Invader") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Zombie Minotaur Warrior"
    power = 2
    toughness = 2
    oracleText = "During your turn, this creature has first strike.\n" +
        "{1}, Sacrifice another creature: This creature gets +2/+0 until end of turn."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.Self),
            condition = Conditions.IsYourTurn,
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.SacrificeAnother(GameObjectFilter.Creature))
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "113"
        artist = "Tomasz Jedruszek"
        flavorText = "\"Give me a crew like this one and I'd rule any sea I sailed.\"\n—Angrath"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2dea2466-5c7f-40ce-b749-100ae89d2c90.jpg?1783933435"
    }
}
