package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shieldhide Dragon
 * {5}{W}
 * Creature — Dragon
 * 3/3
 * Flying, lifelink
 * Megamorph {5}{W}{W}
 * When this creature is turned face up, put a +1/+1 counter on each other Dragon creature you control.
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a +1/+1 counter on
 * the creature when it is turned face up for its megamorph cost. The Dragon pump is a separate
 * "turned face up" trigger, so it fires however the creature is turned face up; "each other" is
 * the group filter's `excludeSelf`.
 */
val ShieldhideDragon = card("Shieldhide Dragon") {
    manaCost = "{5}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dragon"
    power = 3
    toughness = 3
    oracleText = "Flying, lifelink\n" +
        "Megamorph {5}{W}{W} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)\n" +
        "When this creature is turned face up, put a +1/+1 counter on each other Dragon creature " +
        "you control."

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    // Megamorph {5}{W}{W}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{5}{W}{W}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    triggeredAbility {
        trigger = Triggers.self.turnedFaceUp()
        effect = Effects.ForEachInGroup(
            GroupFilter(
                GameObjectFilter.Creature.withSubtype(Subtype.DRAGON).youControl(),
                excludeSelf = true,
            ),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
        )
        description = "When this creature is turned face up, put a +1/+1 counter on each other Dragon " +
            "creature you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "37"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fcfd0af4-44e1-424b-8e55-87f84a42d40a.jpg?1783938612"
        ruling(
            "2015-02-25",
            "If a face-down creature with megamorph is turned face up some other way (for example, " +
                "if you manifest a card with megamorph and then pay its mana cost to turn it face up), " +
                "you won't put a +1/+1 counter on it.",
        )
    }
}
