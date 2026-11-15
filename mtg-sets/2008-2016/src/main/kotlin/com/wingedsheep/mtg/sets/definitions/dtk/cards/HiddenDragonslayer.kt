package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hidden Dragonslayer
 * {1}{W}
 * Creature — Human Warrior
 * 2/1
 * Lifelink
 * Megamorph {2}{W}
 * When this creature is turned face up, destroy target creature with power 4 or greater an
 * opponent controls.
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a +1/+1 counter on
 * the creature when it is turned face up for its megamorph cost. The destroy trigger is a separate
 * "turned face up" trigger, so it also fires when the creature is turned face up some other way.
 */
val HiddenDragonslayer = card("Hidden Dragonslayer") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 1
    oracleText = "Lifelink\n" +
        "Megamorph {2}{W} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)\n" +
        "When this creature is turned face up, destroy target creature with power 4 or greater " +
        "an opponent controls."

    keywords(Keyword.LIFELINK)

    // Megamorph {2}{W}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{2}{W}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    triggeredAbility {
        trigger = Triggers.self.turnedFaceUp()
        val creature = target(TargetFilter.Creature.powerAtLeast(4).opponentControls())
        effect = Effects.Destroy(creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "Anastasia Ovchinnikova"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87c760a8-e051-4584-ae1b-178164b84427.jpg?1783938615"
        ruling(
            "2015-02-25",
            "If a face-down creature with megamorph is turned face up some other way (for example, " +
                "if you manifest a card with megamorph and then pay its mana cost to turn it face up), " +
                "you won't put a +1/+1 counter on it.",
        )
    }
}
