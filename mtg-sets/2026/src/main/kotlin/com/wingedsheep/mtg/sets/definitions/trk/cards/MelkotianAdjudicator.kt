package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Melkotian Adjudicator
 * {2}{U}
 * Creature — Melkotian Advisor
 * 2/1
 *
 * Flying, vigilance
 * Whenever you face a dilemma, put a +1/+1 counter on this creature. (You face a dilemma as you
 * choose one or more modes for a spell or ability.)
 */
val MelkotianAdjudicator = card("Melkotian Adjudicator") {
    manaCost = "{2}{U}"
    typeLine = "Creature — Melkotian Advisor"
    colorIdentity = "U"
    oracleText = "Flying, vigilance\n" +
        "Whenever you face a dilemma, put a +1/+1 counter on this creature. " +
        "(You face a dilemma as you choose one or more modes for a spell or ability.)"
    power = 2
    toughness = 1

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.you.facesADilemma()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "73"
        artist = "Jason Raish"
        flavorText = "The Melkotian court is the mind, and its advocates the phantoms of the past."
        imageUri = "https://cards.scryfall.io/normal/front/8/7/873c9933-7939-4376-873c-6a5b3bbc98d4.jpg?1793359395"
    }
}
