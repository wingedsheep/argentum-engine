package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.opus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tackle Artist
 * {3}{R}
 * Creature — Orc Sorcerer
 * 4/3
 *
 * Trample
 * Opus — Whenever you cast an instant or sorcery spell, put a +1/+1 counter on this creature. If five
 * or more mana was spent to cast that spell, put two +1/+1 counters on this creature instead.
 *
 * "Opus" is an ability word (flavor only); the `opus { }` builder wires the spell-cast trigger and the
 * 5+ mana tier. Here the 5+ mana bonus **replaces** the base counter (`insteadIfFiveOrMore`): one
 * +1/+1 counter normally, two when five or more mana was spent. Both counters persist (real +1/+1
 * counters, not an until-end-of-turn buff).
 */
val TackleArtist = card("Tackle Artist") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc Sorcerer"
    power = 4
    toughness = 3
    oracleText = "Trample\nOpus — Whenever you cast an instant or sorcery spell, put a +1/+1 counter " +
        "on this creature. If five or more mana was spent to cast that spell, put two +1/+1 counters " +
        "on this creature instead."

    keywords(Keyword.TRAMPLE)

    opus {
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        insteadIfFiveOrMore = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
        description = "Opus — Whenever you cast an instant or sorcery spell, put a +1/+1 counter on " +
            "this creature. If five or more mana was spent to cast that spell, put two +1/+1 counters " +
            "on this creature instead."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Ioannis Fiore"
        flavorText = "\"Let's paint the field red!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b87e2474-98c1-4c1a-91ed-340b72d31653.jpg?1775937898"
    }
}
