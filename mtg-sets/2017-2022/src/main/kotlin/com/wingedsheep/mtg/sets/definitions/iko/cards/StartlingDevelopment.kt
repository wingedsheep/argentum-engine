package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Startling Development
 * {1}{U}
 * Instant
 *
 * Until end of turn, target creature becomes a blue Serpent with base power and toughness 4/4.
 * Cycling {1} ({1}, Discard this card: Draw a card.)
 *
 * The Serpentine Ambush shape: [Effects.BecomeCreature] sets base P/T 4/4 (layer 7b), replaces
 * the creature subtypes with Serpent and the colors with blue, all until end of turn.
 */
val StartlingDevelopment = card("Startling Development") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature becomes a blue Serpent with base power and toughness 4/4.\n" +
        "Cycling {1} ({1}, Discard this card: Draw a card.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.BecomeCreature(
            target = creature,
            power = 4,
            toughness = 4,
            creatureTypes = setOf("Serpent"),
            colors = setOf(Color.BLUE.name),
            duration = Duration.EndOfTurn,
        )
    }

    keywordAbility(KeywordAbility.cycling("{1}"))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "68"
        artist = "Simon Dominic"
        flavorText = "Remarkably, dinner continued with no further interruptions."
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a12680db-957f-48c1-9062-2edd9115ba26.jpg"
    }
}
