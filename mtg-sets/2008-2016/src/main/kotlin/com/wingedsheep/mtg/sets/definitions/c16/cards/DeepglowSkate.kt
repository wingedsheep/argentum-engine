package com.wingedsheep.mtg.sets.definitions.c16.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Deepglow Skate
 * {4}{U}
 * Creature — Fish
 * 3/3
 * When this creature enters, double the number of each kind of counter on any number of target
 * permanents.
 *
 * "Any number of target permanents" is an unlimited, zero-floor requirement fanned out by
 * [Effects.ForEachTarget]; each chosen permanent has every kind of counter on it doubled by
 * [Effects.DoubleAllCounters] (Zimone, Paradox Sculptor's shape), each kind placed separately so
 * counter-placement replacements apply per the ruling.
 */
val DeepglowSkate = card("Deepglow Skate") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Fish"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, double the number of each kind of counter on any number of " +
        "target permanents."

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.Permanent, unlimited = true)
        effect = Effects.ForEachTarget(
            Effects.DoubleAllCounters(EffectTarget.ContextTarget(0))
        )
        description = "When this creature enters, double the number of each kind of counter on any number " +
            "of target permanents."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Jason Kang"
        flavorText = "Its graceful glide paints the ocean floor with strokes of luminescence."
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f7c787f-2134-4cfd-901f-d5e45e62de1e.jpg?1783937090"
        ruling(
            "2020-08-07",
            "To double the number of each kind of counter on a permanent, put another counter on it for each " +
                "counter it already has. Effects that interact with counters being put onto permanents apply " +
                "as appropriate."
        )
        ruling("2020-08-07", "You can choose no targets if you don't want to double the counters on any permanents.")
    }
}
