package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Bearer of Glory
 * {1}{W}
 * Creature — Human Soldier
 * 2/1
 *
 * During your turn, this creature has first strike.
 * {4}{W}: Creatures you control get +1/+1 until end of turn.
 */
val BearerOfGlory = card("Bearer of Glory") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 1
    oracleText = "During your turn, this creature has first strike.\n{4}{W}: Creatures you control get +1/+1 until end of turn."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.FIRST_STRIKE, Filters.Self),
            condition = Conditions.IsYourTurn
        )
    }

    activatedAbility {
        cost = Costs.Mana("{4}{W}")
        effect = EffectPatterns.modifyStatsForAll(1, 1, GroupFilter.AllCreaturesYouControl)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "Joshua Cairos"
        flavorText = "Morale stands so long as he does."
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6d91e42-43db-428d-a4dd-ef9d40306314.jpg?1743203967"
    }
}
