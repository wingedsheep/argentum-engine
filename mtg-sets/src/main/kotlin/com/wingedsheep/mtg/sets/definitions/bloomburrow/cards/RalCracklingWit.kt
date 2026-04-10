package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ral, Crackling Wit {2}{U}{R}
 * Legendary Planeswalker — Ral
 * Starting Loyalty: 4
 *
 * Whenever you cast a noncreature spell, put a loyalty counter on Ral.
 * +1: Create a 1/1 blue and red Otter creature token with prowess.
 * −3: Draw three cards, then discard two cards.
 * −10: Draw three cards. You get an emblem with "Instant and sorcery spells you cast have storm."
 */
val RalCracklingWit = card("Ral, Crackling Wit") {
    manaCost = "{2}{U}{R}"
    typeLine = "Legendary Planeswalker — Ral"
    startingLoyalty = 4
    oracleText = "Whenever you cast a noncreature spell, put a loyalty counter on Ral, Crackling Wit.\n+1: Create a 1/1 blue and red Otter creature token with prowess.\n\u22123: Draw three cards, then discard two cards.\n\u221210: Draw three cards. You get an emblem with \"Instant and sorcery spells you cast have storm.\""

    // Whenever you cast a noncreature spell, put a loyalty counter on Ral.
    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = Effects.AddCounters(Counters.LOYALTY, 1, EffectTarget.Self)
    }

    // +1: Create a 1/1 blue and red Otter creature token with prowess.
    loyaltyAbility(+1) {
        effect = CreateTokenEffect(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE, Color.RED),
            creatureTypes = setOf("Otter"),
            keywords = setOf(Keyword.PROWESS),
            imageUri = "https://cards.scryfall.io/normal/front/e/6/e6b2c465-c446-4dee-9101-763105dcf813.jpg?1724438155"
        )
    }

    // −3: Draw three cards, then discard two cards.
    loyaltyAbility(-3) {
        effect = Effects.DrawCards(3).then(EffectPatterns.discardCards(2))
    }

    // −10: Draw three cards. You get an emblem with "Instant and sorcery spells you cast have storm."
    loyaltyAbility(-10) {
        effect = CompositeEffect(
            listOf(
                Effects.DrawCards(3),
                Effects.GrantSpellKeyword(Keyword.STORM, GameObjectFilter.InstantOrSorcery)
            )
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "230"
        artist = "Rudy Siswanto"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acfde780-899a-4c5b-a39b-f4a3ff129103.jpg?1721427178"

        ruling("2024-07-26", "Ral's first ability resolves before the spell that caused it to trigger. It resolves even if that spell is countered.")
        ruling("2024-07-26", "The copies of instant and sorcery spells you cast created by the storm ability are put directly onto the stack. They aren't cast and won't be counted by other spells with storm cast later in the turn.")
    }
}
