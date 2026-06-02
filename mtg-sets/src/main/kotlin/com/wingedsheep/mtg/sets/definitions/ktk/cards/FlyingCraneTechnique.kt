package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.dsl.Effects

/**
 * Flying Crane Technique
 * {3}{U}{R}{W}
 * Instant
 * Untap all creatures you control. They gain flying and double strike until end of turn.
 */
val FlyingCraneTechnique = card("Flying Crane Technique") {
    manaCost = "{3}{U}{R}{W}"
    colorIdentity = "WUR"
    typeLine = "Instant"
    oracleText = "Untap all creatures you control. They gain flying and double strike until end of turn."

    spell {
        effect = Effects.Composite(
            listOf(
                Effects.ForEachInGroup(
                    GroupFilter.AllCreaturesYouControl,
                    TapUntapEffect(EffectTarget.Self, tap = false)
                ),
                Effects.ForEachInGroup(
                    GroupFilter.AllCreaturesYouControl,
                    GrantKeywordEffect(Keyword.FLYING, EffectTarget.Self)
                ),
                Effects.ForEachInGroup(
                    GroupFilter.AllCreaturesYouControl,
                    GrantKeywordEffect(Keyword.DOUBLE_STRIKE, EffectTarget.Self)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "176"
        artist = "Jack Wang"
        flavorText = "There are many Jeskai styles: Riverwalk imitates flowing water, Dragonfist the ancient hellkites, and Flying Crane the wild aven of the high peaks."
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1ba7efe-ae5a-4d11-b535-1c2e5e6f5982.jpg?1562795844"
    }
}
