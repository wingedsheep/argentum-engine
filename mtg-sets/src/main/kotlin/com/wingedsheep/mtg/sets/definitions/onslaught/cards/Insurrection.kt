package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect

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
            ForEachInGroupEffect(GroupFilter.AllCreatures, GainControlEffect(EffectTarget.Self, Duration.EndOfTurn)),
            ForEachInGroupEffect(GroupFilter.AllCreatures, TapUntapEffect(EffectTarget.Self, tap = false)),
            ForEachInGroupEffect(GroupFilter.AllCreatures, GrantKeywordUntilEndOfTurnEffect(Keyword.HASTE, EffectTarget.Self, Duration.EndOfTurn))
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
