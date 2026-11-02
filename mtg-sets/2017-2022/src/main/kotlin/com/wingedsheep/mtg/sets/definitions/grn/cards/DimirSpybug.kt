package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dimir Spybug
 * {U}{B}
 * Creature — Insect
 * 1/1
 * Flying
 * Menace (This creature can't be blocked except by two or more creatures.)
 * Whenever you surveil, put a +1/+1 counter on this creature.
 */
val DimirSpybug = card("Dimir Spybug") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Insect"
    oracleText = "Flying\n" +
        "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Whenever you surveil, put a +1/+1 counter on this creature."
    power = 1
    toughness = 1

    keywords(Keyword.FLYING, Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.you.surveils()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "166"
        artist = "James Paick"
        flavorText = "Just assume every conversation is on record."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c18c450-0bfb-4ba6-9212-b5833fce63a2.jpg?1783934137"
    }
}
