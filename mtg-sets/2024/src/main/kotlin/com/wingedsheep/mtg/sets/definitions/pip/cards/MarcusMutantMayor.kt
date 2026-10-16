package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Marcus, Mutant Mayor
 * {3}{G}{U}
 * Legendary Creature — Mutant Advisor, 4/4
 *
 * Vigilance, trample
 * Whenever a creature you control deals combat damage to a player, draw a card if that creature
 * has a +1/+1 counter on it. If it doesn't, put a +1/+1 counter on it.
 *
 * The counter check happens on resolution against the triggering creature.
 */
val MarcusMutantMayor = card("Marcus, Mutant Mayor") {
    manaCost = "{3}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Legendary Creature — Mutant Advisor"
    oracleText = "Vigilance, trample\nWhenever a creature you control deals combat damage to a player, draw a card if that creature has a +1/+1 counter on it. If it doesn't, put a +1/+1 counter on it."
    power = 4
    toughness = 4
    keywords(Keyword.VIGILANCE, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.If(
            condition = Conditions.TargetHasCounter(CounterType.PLUS_ONE_PLUS_ONE, EffectTarget.TriggeringEntity),
            then = Effects.DrawCards(1),
            otherwise = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "109"
        artist = "Josu Hernaiz"
        flavorText = "\"Welcome to Jacobstown, human. You're free to walk around. Just don't stare at the nightkin.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/5/15db5769-af10-4369-ab4c-db25adb22223.jpg?1783912387"
    }
}
