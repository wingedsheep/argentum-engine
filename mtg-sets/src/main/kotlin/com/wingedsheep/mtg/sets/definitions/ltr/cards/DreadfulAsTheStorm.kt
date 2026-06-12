package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration

/**
 * Dreadful as the Storm
 * {2}{U}
 * Instant
 *
 * Target creature has base power and toughness 5/5 until end of turn. The Ring tempts you.
 *
 * Gap 13 (set base power AND toughness) is engine-landed: SetBasePowerToughnessEffect + its
 * executor already exist; this adds the `Effects.SetBasePowerAndToughness` facade and composes it
 * with the Ring-tempts rider.
 */
val DreadfulAsTheStorm = card("Dreadful as the Storm") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target creature has base power and toughness 5/5 until end of turn. The Ring tempts you."

    spell {
        val creature = target("creature", Targets.Creature)
        effect = Effects.SetBasePowerAndToughness(5, 5, creature, Duration.EndOfTurn)
            .then(Effects.TheRingTemptsYou())
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Daniel Correia"
        flavorText = "\"I shall not be dark, but beautiful and terrible as the Morning and the Night! All shall love me and despair!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6e0b4ff-85b5-485f-a78f-1a58bb343238.jpg?1686968073"
    }
}
