package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Silumgar Spell-Eater
 * {2}{U}
 * Creature — Snake Wizard
 * 2/3
 * Megamorph {4}{U}
 * When this creature is turned face up, counter target spell unless its controller pays {3}.
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a +1/+1 counter on
 * the creature when it is turned face up for its megamorph cost. The counter trigger is a separate
 * "turned face up" trigger, so it also fires when the creature is turned face up some other way.
 */
val SilumgarSpellEater = card("Silumgar Spell-Eater") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Snake Wizard"
    power = 2
    toughness = 3
    oracleText = "Megamorph {4}{U} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)\n" +
        "When this creature is turned face up, counter target spell unless its controller pays {3}."

    // Megamorph {4}{U}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{4}{U}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    triggeredAbility {
        trigger = Triggers.self.turnedFaceUp()
        val spell = target(TargetFilter.SpellOnStack)
        effect = Effects.CounterUnlessPays("{3}")
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "77"
        artist = "Dave Kendall"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20a99655-34b2-4756-bf12-b344eb71ad37.jpg?1783938602"
        ruling(
            "2015-02-25",
            "If a face-down creature with megamorph is turned face up some other way (for example, " +
                "if you manifest a card with megamorph and then pay its mana cost to turn it face up), " +
                "you won't put a +1/+1 counter on it.",
        )
    }
}
