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
 * Kolaghan Stormsinger
 * {R}
 * Creature — Human Shaman
 * 1/1
 * Haste
 * Megamorph {R}
 * When this creature is turned face up, target creature gains haste until end of turn.
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a +1/+1 counter on
 * the creature when it is turned face up for its megamorph cost. The haste grant is a separate
 * "turned face up" trigger, so it fires however the creature is turned face up; any creature
 * (including Kolaghan Stormsinger itself, or an opponent's creature) is a legal target.
 */
val KolaghanStormsinger = card("Kolaghan Stormsinger") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    power = 1
    toughness = 1
    oracleText = "Haste\n" +
        "Megamorph {R} (You may cast this card face down as a 2/2 creature for {3}. " +
        "Turn it face up any time for its megamorph cost and put a +1/+1 counter on it.)\n" +
        "When this creature is turned face up, target creature gains haste until end of turn."

    keywords(Keyword.HASTE)

    // Megamorph {R}: turn face up for the cost, then put a +1/+1 counter on it.
    morph = "{R}"
    morphFaceUpEffect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)

    triggeredAbility {
        trigger = Triggers.self.turnedFaceUp()
        val t = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.HASTE, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "145"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b6723f5-a7a9-425d-b4b0-f380da578ef8.jpg?1783938588"
        ruling(
            "2015-02-25",
            "If a face-down creature with megamorph is turned face up some other way (for example, " +
                "if you manifest a card with megamorph and then pay its mana cost to turn it face up), " +
                "you won't put a +1/+1 counter on it.",
        )
    }
}
