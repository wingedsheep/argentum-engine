package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Soul Swallower (Shadows over Innistrad #230)
 * {2}{G}{G}
 * Creature — Wurm
 * 3/3
 *
 * Trample
 * Delirium — At the beginning of your upkeep, if there are four or more card types among cards in
 * your graveyard, put three +1/+1 counters on this creature.
 */
val SoulSwallower = card("Soul Swallower") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm"
    power = 3
    toughness = 3
    oracleText = "Trample\n" +
        "Delirium — At the beginning of your upkeep, if there are four or more card types among cards in " +
        "your graveyard, put three +1/+1 counters on this creature."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.Delirium()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 3, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "230"
        artist = "Marco Nelor"
        flavorText = "Being engulfed is just the beginning of the nightmare."
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22fbc045-2c86-4816-8622-db56929ae94d.jpg"
    }
}
