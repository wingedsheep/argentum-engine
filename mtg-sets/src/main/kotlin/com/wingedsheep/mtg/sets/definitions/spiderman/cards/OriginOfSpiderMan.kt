package com.wingedsheep.mtg.sets.definitions.spiderman.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.AddCardTypeEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect

/**
 * Origin of Spider-Man
 * {1}{W}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Create a 2/1 green Spider creature token with reach.
 * II — Put a +1/+1 counter on target creature you control. It becomes a legendary Spider
 *      Hero in addition to its other types.
 * III — Target creature you control gains double strike until end of turn.
 */
val OriginOfSpiderMan = card("Origin of Spider-Man") {
    manaCost = "{1}{W}"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Create a 2/1 green Spider creature token with reach.\n" +
        "II — Put a +1/+1 counter on target creature you control. It becomes a legendary Spider Hero in addition to its other types.\n" +
        "III — Target creature you control gains double strike until end of turn."

    // I — Create a 2/1 green Spider creature token with reach.
    sagaChapter(1) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Spider"),
            keywords = setOf(Keyword.REACH),
            imageUri = "https://cards.scryfall.io/normal/front/4/a/4a40f6e1-3545-4503-af3e-f0acfb735e3a.jpg?1757379309"
        )
    }

    // II — +1/+1 counter + becomes a legendary Spider Hero.
    sagaChapter(2) {
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = CompositeEffect(listOf(
            AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, creature),
            AddCardTypeEffect("LEGENDARY", creature, Duration.Permanent),
            Effects.AddCreatureType("Spider", creature, Duration.Permanent),
            Effects.AddCreatureType("Hero", creature, Duration.Permanent)
        ))
    }

    // III — Target creature you control gains double strike until end of turn.
    sagaChapter(3) {
        val creature = target("creature you control", Targets.CreatureYouControl)
        effect = GrantKeywordEffect(Keyword.DOUBLE_STRIKE.name, creature, Duration.EndOfTurn)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "Bill Sienkiewicz"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a10a7da7-d9cb-495a-9c9f-205d355c390d.jpg?1757376788"
    }
}
