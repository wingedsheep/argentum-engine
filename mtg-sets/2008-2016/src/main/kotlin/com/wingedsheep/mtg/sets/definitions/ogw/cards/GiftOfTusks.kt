package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Gift of Tusks
 * {U}
 * Instant
 * Until end of turn, target creature loses all abilities and becomes a green Elephant with base
 * power and toughness 3/3.
 *
 * The until-end-of-turn form of Lizard, Connors's Curse's shape: [Effects.RemoveAllAbilities]
 * (Layer 6) then [Effects.BecomeCreature] with `creatureTypes` (Layer 4 set — it loses all other
 * creature types), `colors` (Layer 5 set — it loses all other colors) and base 3/3 (Layer 7b).
 * BecomeCreature only *adds* CREATURE and removes nothing, so an artifact creature stays an
 * artifact creature and a legendary one stays legendary, as the 2016-01-22 ruling requires.
 * Counters and pumps still apply on top of the base 3/3.
 */
val GiftOfTusks = card("Gift of Tusks") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature loses all abilities and becomes a green Elephant " +
        "with base power and toughness 3/3."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.RemoveAllAbilities(creature, Duration.EndOfTurn) then
            Effects.BecomeCreature(
                target = creature,
                power = 3,
                toughness = 3,
                creatureTypes = setOf("Elephant"),
                colors = setOf("GREEN"),
                duration = Duration.EndOfTurn
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Kev Walker"
        flavorText = "It's an experience you'll never forget."
        imageUri = "https://cards.scryfall.io/normal/front/3/8/3821122c-c495-42b0-b8f6-3f469967f16d.jpg?1783937918"
        ruling("2016-01-22", "The creature will lose all other colors and creature types, but it will retain any other card types (such as artifact) or supertypes (such as legendary) it may have.")
        ruling("2016-01-22", "Gift of Tusks overwrites all previous effects that set the creature's base power and toughness to specific values. Any power- or toughness-setting effects that start to apply after Gift of Tusks resolves will overwrite this effect.")
        ruling("2016-01-22", "If the affected creature gains an ability after Gift of Tusks resolves, it will keep that ability.")
        ruling("2016-01-22", "Effects that modify a creature's power and/or toughness, such as the effect of Titanic Growth, will apply to the creature no matter when they started to take effect. The same is true for any counters that change its power and/or toughness and effects that switch its power and toughness.")
    }
}
