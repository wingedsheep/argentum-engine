package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Sunshower Druid
 * {G}
 * Creature — Frog Druid
 * 0/2
 *
 * When this creature enters, put a +1/+1 counter on target creature and you gain 1 life.
 */
val SunshowerDruid = card("Sunshower Druid") {
    manaCost = "{G}"
    typeLine = "Creature — Frog Druid"
    power = 0
    toughness = 2
    oracleText = "When this creature enters, put a +1/+1 counter on target creature and you gain 1 life."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("target creature", Targets.Creature)
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, creature)
            .then(Effects.GainLife(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "195"
        artist = "Tuan Duong Chu"
        flavorText = "Apprentice augurs begin their studies by predicting where the rain will fall. Even those who don't succeed derive great joy from the process."
        imageUri = "https://cards.scryfall.io/normal/front/7/7/7740abc5-54e1-478d-966e-0fa64e727995.jpg?1721426936"
    }
}
