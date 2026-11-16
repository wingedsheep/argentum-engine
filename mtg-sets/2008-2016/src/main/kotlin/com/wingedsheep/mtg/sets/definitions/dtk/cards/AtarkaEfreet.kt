package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Atarka Efreet
 * {3}{R}
 * Creature — Efreet Shaman
 * 5/1
 * Megamorph {2}{R}
 * When this creature is turned face up, it deals 1 damage to any target.
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a +1/+1 counter on
 * the creature when it is turned face up for its megamorph cost. The damage is a separate
 * "turned face up" trigger, so it fires however the creature is turned face up.
 */
val AtarkaEfreet = card("Atarka Efreet") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Efreet Shaman"
    power = 5
    toughness = 1
    oracleText = "Megamorph {2}{R} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)\n" +
        "When this creature is turned face up, it deals 1 damage to any target."

    // Megamorph {2}{R}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{2}{R}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    triggeredAbility {
        trigger = Triggers.self.turnedFaceUp()
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "128"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b192067c-7cdb-4d3c-95b7-819733e44678.jpg?1783938592"
        ruling(
            "2015-02-25",
            "If a face-down creature with megamorph is turned face up some other way (for example, " +
                "if you manifest a card with megamorph and then pay its mana cost to turn it face up), " +
                "you won't put a +1/+1 counter on it.",
        )
    }
}
