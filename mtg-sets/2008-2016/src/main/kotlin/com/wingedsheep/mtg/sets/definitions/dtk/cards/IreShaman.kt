package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ire Shaman
 * {1}{R}
 * Creature — Orc Shaman
 * 2/1
 * Menace
 * Megamorph {R}
 * When this creature is turned face up, exile the top card of your library. Until end of turn,
 * you may play that card.
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a +1/+1 counter on
 * the creature when it is turned face up for its megamorph cost. The impulse draw is a separate
 * "turned face up" trigger, so it fires however the creature is turned face up;
 * [Patterns.Exile.impulse] exiles the top card face up and lets you play it (paying its costs,
 * following normal timing) until end of turn.
 */
val IreShaman = card("Ire Shaman") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc Shaman"
    power = 2
    toughness = 1
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Megamorph {R} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)\n" +
        "When this creature is turned face up, exile the top card of your library. " +
        "Until end of turn, you may play that card."

    keywords(Keyword.MENACE)

    // Megamorph {R}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{R}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    triggeredAbility {
        trigger = Triggers.self.turnedFaceUp()
        effect = Patterns.Exile.impulse(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "141"
        artist = "Jack Wang"
        imageUri = "https://cards.scryfall.io/normal/front/1/8/18f8ac7a-a68a-4adf-995f-1cd96ee3d295.jpg?1783938589"
        ruling(
            "2015-02-25",
            "If a face-down Ire Shaman attacks and is blocked by one creature, turning it face up " +
                "won't cause it to become unblocked.",
        )
        ruling(
            "2015-02-25",
            "If a face-down creature with megamorph is turned face up some other way (for example, " +
                "if you manifest a card with megamorph and then pay its mana cost to turn it face up), " +
                "you won't put a +1/+1 counter on it.",
        )
    }
}
