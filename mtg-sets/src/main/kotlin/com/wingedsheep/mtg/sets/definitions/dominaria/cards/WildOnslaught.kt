package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val WildOnslaught = card("Wild Onslaught") {
    manaCost = "{3}{G}"
    typeLine = "Instant"
    oracleText = "Kicker {4} (You may pay an additional {4} as you cast this spell.)\nPut a +1/+1 counter on each creature you control. If this spell was kicked, put two +1/+1 counters on each creature you control instead."

    keywordAbility(KeywordAbility.Kicker(ManaCost.parse("{4}")))

    spell {
        effect = ConditionalEffect(
            condition = WasKicked,
            effect = ForEachInGroupEffect(
                filter = GroupFilter.AllCreaturesYouControl,
                effect = AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
            ),
            elseEffect = ForEachInGroupEffect(
                filter = GroupFilter.AllCreaturesYouControl,
                effect = AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "188"
        artist = "Simon Dominic"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb75cf21-08be-4b92-bdf6-014a36090738.jpg?1562744928"
    }
}
