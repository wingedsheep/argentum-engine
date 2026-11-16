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
 * Stratus Dancer
 * {1}{U}
 * Creature — Djinn Monk
 * 2/1
 * Flying
 * Megamorph {1}{U}
 * When this creature is turned face up, counter target instant or sorcery spell.
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a +1/+1 counter on
 * the creature when it is turned face up for its megamorph cost. The counter trigger is a separate
 * "turned face up" trigger, so it also fires when the creature is turned face up some other way.
 */
val StratusDancer = card("Stratus Dancer") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Djinn Monk"
    power = 2
    toughness = 1
    oracleText = "Flying\n" +
        "Megamorph {1}{U} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)\n" +
        "When this creature is turned face up, counter target instant or sorcery spell."

    keywords(Keyword.FLYING)

    // Megamorph {1}{U}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{1}{U}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    triggeredAbility {
        trigger = Triggers.self.turnedFaceUp()
        val spell = target(TargetFilter.InstantOrSorcerySpellOnStack)
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "80"
        artist = "Anastasia Ovchinnikova"
        imageUri = "https://cards.scryfall.io/normal/front/e/3/e3bf4f37-2a2e-4c49-88dc-a26dd7367afb.jpg?1783938602"
        ruling(
            "2015-02-25",
            "If a face-down creature with megamorph is turned face up some other way (for example, " +
                "if you manifest a card with megamorph and then pay its mana cost to turn it face up), " +
                "you won't put a +1/+1 counter on it.",
        )
    }
}
