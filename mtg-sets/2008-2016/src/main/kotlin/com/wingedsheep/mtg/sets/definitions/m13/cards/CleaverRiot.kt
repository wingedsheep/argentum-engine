package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cleaver Riot
 * {4}{R}
 * Sorcery
 * Creatures you control gain double strike until end of turn.
 */
val CleaverRiot = card("Cleaver Riot") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Creatures you control gain double strike until end of turn. (They deal both first-strike and regular combat damage.)"

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Dave Kendall"
        flavorText = "Measure once, cut twice."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/6761eacf-03fc-4ccd-a4a6-eca5357b5c5b.jpg"
    }
}
