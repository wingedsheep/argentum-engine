package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val JuniperOrderRootweaver = card("Juniper Order Rootweaver") {
    manaCost = "{1}{W}"
    colorIdentity = "GW"
    typeLine = "Creature — Human Druid"
    power = 2
    toughness = 2
    oracleText = "Kicker {G} (You may pay an additional {G} as you cast this spell.)\nWhen this creature enters, if it was kicked, put a +1/+1 counter on target creature you control."

    keywordAbility(KeywordAbility.kicker("{G}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = WasKicked
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Matt Stewart"
        flavorText = "\"What Phyrexia takes, the grace of Freyalise shall replenish.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c56cf82f-8bbb-49ae-ad93-291cbec9126e.jpg?1783921364"
    }
}
