package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Guardian Shield-Bearer
 * {1}{G}
 * Creature — Human Soldier
 * 2/1
 * Megamorph {3}{G}
 * When this creature is turned face up, put a +1/+1 counter on another target creature you control.
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a +1/+1 counter on
 * the creature when it is turned face up for its megamorph cost. The counter trigger is a separate
 * "turned face up" trigger, so it also fires when the creature is turned face up some other way.
 */
val GuardianShieldBearer = card("Guardian Shield-Bearer") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 1
    oracleText = "Megamorph {3}{G} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)\n" +
        "When this creature is turned face up, put a +1/+1 counter on another target creature you control."

    // Megamorph {3}{G}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{3}{G}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    triggeredAbility {
        trigger = Triggers.self.turnedFaceUp()
        val creature = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
        description = "When this creature is turned face up, put a +1/+1 counter on another target creature you control."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "189"
        artist = "Lindsey Look"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dcbd7958-29a0-4f59-9648-eabe0fbfb4f9.jpg?1783938579"
        ruling(
            "2015-02-25",
            "If a face-down creature with megamorph is turned face up some other way (for example, " +
                "if you manifest a card with megamorph and then pay its mana cost to turn it face up), " +
                "you won't put a +1/+1 counter on it.",
        )
    }
}
