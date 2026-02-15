package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GainControlOfGroupEffect
import com.wingedsheep.sdk.scripting.GrantKeywordToGroupEffect
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.UntapGroupEffect

/**
 * Insurrection
 * {5}{R}{R}{R}
 * Sorcery
 * Untap all creatures and gain control of them until end of turn.
 * They gain haste until end of turn.
 */
val Insurrection = card("Insurrection") {
    manaCost = "{5}{R}{R}{R}"
    typeLine = "Sorcery"
    oracleText = "Untap all creatures and gain control of them until end of turn. They gain haste until end of turn."

    spell {
        effect = Effects.Composite(
            GainControlOfGroupEffect(GroupFilter.AllCreatures, Duration.EndOfTurn),
            UntapGroupEffect(GroupFilter.AllCreatures),
            GrantKeywordToGroupEffect(Keyword.HASTE, GroupFilter.AllCreatures, Duration.EndOfTurn)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "213"
        artist = "Mark Zug"
        flavorText = "\"Maybe they wanted to be on the winning side for once.\" —Matoc, lavamancer"
        imageUri = "https://cards.scryfall.io/large/front/9/9/998bad32-1927-4e12-9527-efa55b86cae0.jpg?1562931187"
    }
}
