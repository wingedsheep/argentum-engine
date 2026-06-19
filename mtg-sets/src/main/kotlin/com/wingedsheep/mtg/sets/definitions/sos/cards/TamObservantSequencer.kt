package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tam, Observant Sequencer // Deep Sight — Secrets of Strixhaven #237
 * {2}{G}{U} · Legendary Creature — Gorgon Wizard · 4/3
 *
 * Landfall — Whenever a land you control enters, Tam becomes prepared. (While it's prepared, you
 * may cast a copy of its spell. Doing so unprepares it.)
 * //
 * Deep Sight — {G}{U}, Sorcery: You draw a card and gain 1 life.
 *
 * Prepare (Secrets of Strixhaven): like Encouraging Aviator, Tam does NOT enter prepared (no
 * PREPARED keyword). It only becomes prepared via its landfall trigger through
 * [Effects.BecomePrepared]. Becoming prepared creates a copy of its prepare spell ("Deep Sight")
 * in exile that its controller may cast for {G}{U}; casting that copy unprepares Tam. Modeled via
 * [com.wingedsheep.sdk.model.CardLayout.PREPARE] + the `prepare(name) { }` DSL.
 */
val TamObservantSequencer = card("Tam, Observant Sequencer") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "UG"
    typeLine = "Legendary Creature — Gorgon Wizard"
    power = 4
    toughness = 3
    oracleText = "Landfall — Whenever a land you control enters, Tam becomes prepared. (While " +
        "it's prepared, you may cast a copy of its spell. Doing so unprepares it.)"

    // Landfall — whenever a land you control enters, Tam becomes prepared.
    triggeredAbility {
        trigger = Triggers.LandYouControlEnters
        effect = Effects.BecomePrepared(EffectTarget.Self)
        description = "Landfall — Whenever a land you control enters, Tam becomes prepared."
    }

    // Deep Sight — the prepare spell. You draw a card and gain 1 life.
    prepare("Deep Sight") {
        manaCost = "{G}{U}"
        typeLine = "Sorcery"
        oracleText = "You draw a card and gain 1 life."
        spell {
            effect = Effects.Composite(
                Effects.DrawCards(1),
                Effects.GainLife(1),
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "237"
        artist = "Jodie Muir"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/7120e71b-2976-451b-89a7-a1665dc6fb6b.jpg?1778165018"
    }
}
