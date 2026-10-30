package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flourishing Fox
 * {W}
 * Creature — Fox
 * 1/1
 * Whenever you cycle another card, put a +1/+1 counter on this creature.
 * Cycling {1} ({1}, Discard this card: Draw a card.)
 *
 * "Another" needs no filter (see Drannith Healer): the trigger only functions on the
 * battlefield, and a card can only be cycled from hand.
 */
val FlourishingFox = card("Flourishing Fox") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Fox"
    power = 1
    toughness = 1
    oracleText = "Whenever you cycle another card, put a +1/+1 counter on this creature.\n" +
        "Cycling {1} ({1}, Discard this card: Draw a card.)"

    triggeredAbility {
        trigger = Triggers.you.cycles()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever you cycle another card, put a +1/+1 counter on this creature."
    }

    keywordAbility(KeywordAbility.cycling("{1}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Ilse Gort"
        flavorText = "\"Day 1: These fox pups have really taken to me.\n\"Day 4: Big. So big. Need additional security.\"\n—Jonald, mission naturalist, research notes"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78b81339-746d-4f71-9943-01c0f6a5683a.jpg?1783931091"
    }
}
