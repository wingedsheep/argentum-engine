package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val StrengthOfTheCoalition = card("Strength of the Coalition") {
    manaCost = "{G}"
    colorIdentity = "WG"
    typeLine = "Instant"
    oracleText = "Kicker {2}{W} (You may pay an additional {2}{W} as you cast this spell.)\nTarget creature you control gets +2/+2 until end of turn. If this spell was kicked, put a +1/+1 counter on each creature you control."

    keywordAbility(KeywordAbility.kicker("{2}{W}"))

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(2, 2, creature) then Effects.If(
            Conditions.WasKicked,
            Effects.ForEachInGroup(
                GroupFilter.AllCreaturesYouControl,
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "180"
        artist = "Justine Cruz"
        flavorText = "\"Root and wing, claw and sword; this will be a memorable stand, indeed.\"\n—Lyra Dawnbringer"
        imageUri = "https://cards.scryfall.io/normal/front/8/4/8418c504-24fe-470f-8102-dcad68ba1520.jpg?1783921294"

        ruling("2022-09-09", "If the target creature is an illegal target as Strength of the Coalition tries to resolve, none of its effects will occur. You won't put a +1/+1 counter on any creatures, even if it was kicked.")

        ruling("2022-09-09", "If the spell is kicked, the target creature will get both a +1/+1 counter and +2/+2 until end of turn.")
    }
}
