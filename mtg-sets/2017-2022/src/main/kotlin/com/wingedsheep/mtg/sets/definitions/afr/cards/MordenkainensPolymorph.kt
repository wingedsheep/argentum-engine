package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Mordenkainen's Polymorph
 * {1}{U}
 * Instant
 *
 * Until end of turn, target creature becomes a Dragon with base power and toughness 4/4 and
 * gains flying. (It loses all other creature types.)
 *
 * Replaces the creature's subtypes (Layer 4), sets its base P/T (Layer 7b) and grants flying
 * (Layer 6), all until end of turn — the same become-creature stack as Secret Identity. Its
 * abilities and colors are untouched.
 */
val MordenkainensPolymorph = card("Mordenkainen's Polymorph") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature becomes a Dragon with base power and toughness 4/4 " +
        "and gains flying. (It loses all other creature types.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.SetCreatureSubtypes(
            subtypes = setOf("Dragon"),
            target = creature,
            duration = Duration.EndOfTurn
        ) then
            Effects.SetBasePowerAndToughness(
                power = 4,
                toughness = 4,
                target = creature,
                duration = Duration.EndOfTurn
            ) then
            Effects.GrantKeyword(
                keyword = Keyword.FLYING,
                target = creature,
                duration = Duration.EndOfTurn
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "65"
        artist = "Oriana Menendez"
        flavorText = "\"Your purpose may change along with your shape, but you will serve regardless.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6551442-d74c-4f16-a9de-0cfdc88208a5.jpg?1783926511"
        ruling("2021-07-23", "The creature doesn't lose any of its abilities.")
        ruling(
            "2021-07-23",
            "Mordenkainen's Polymorph will overwrite any previous effects that set the creature's power and " +
                "toughness to specific values. Effects that otherwise modify the target creature's power and " +
                "toughness will still apply no matter when they took effect. The same is true for +1/+1 counters."
        )
    }
}
