package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Surrak and Goreclaw
 * {4}{G}{G}
 * Legendary Creature — Human Bear
 * 6/5
 * Trample
 * Other creatures you control have trample.
 * Whenever another nontoken creature you control enters, put a +1/+1 counter on it. It gains haste
 * until end of turn.
 */
val SurrakAndGoreclaw = card("Surrak and Goreclaw") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Bear"
    oracleText = "Trample\n" +
        "Other creatures you control have trample.\n" +
        "Whenever another nontoken creature you control enters, put a +1/+1 counter on it. " +
        "It gains haste until end of turn."
    power = 6
    toughness = 5

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.OtherCreaturesYouControl)
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.nontoken().youControl()).enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity) then
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.TriggeringEntity, Duration.EndOfTurn)
        description = "Whenever another nontoken creature you control enters, put a +1/+1 counter on it. " +
            "It gains haste until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "337"
        artist = "Lucas Graciano"
        flavorText = "Two titans of Tarkir carved a swath through Phyrexia's elite."
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9c10934d-9016-43c4-a7ab-56cc7d8f671f.jpg?1783916899"
    }
}
