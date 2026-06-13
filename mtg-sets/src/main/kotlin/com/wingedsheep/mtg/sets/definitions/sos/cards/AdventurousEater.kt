package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Adventurous Eater // Have a Bite — Secrets of Strixhaven #72
 * {2}{B} · Creature — Human Warlock · 3/2
 *
 * This creature enters prepared. (While it's prepared, you may cast a copy of its spell.
 * Doing so unprepares it.)
 * //
 * Have a Bite — {B}, Sorcery: Put a +1/+1 counter on target creature. You gain 1 life.
 *
 * Prepare (Secrets of Strixhaven): the creature enters with the PREPARED keyword. Becoming
 * prepared creates a copy of its prepare spell ("Have a Bite") in exile that its controller may
 * cast for {B}; casting that copy unprepares the creature. Modeled via [CardLayout.PREPARE] +
 * the `prepare(name) { }` DSL.
 */
val AdventurousEater = card("Adventurous Eater") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warlock"
    power = 3
    toughness = 2
    oracleText = "This creature enters prepared. (While it's prepared, you may cast a copy of its spell. Doing so unprepares it.)"

    keywords(Keyword.PREPARED)

    // Have a Bite — the prepare spell. Put a +1/+1 counter on target creature; you gain 1 life.
    prepare("Have a Bite") {
        manaCost = "{B}"
        typeLine = "Sorcery"
        oracleText = "Put a +1/+1 counter on target creature. You gain 1 life."
        spell {
            target = Targets.Creature
            effect = Effects.Composite(
                Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
                Effects.GainLife(1)
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Josu Hernaiz"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d40cc7da-c731-418e-8547-7033d1939450.jpg?1775937412"
    }
}
