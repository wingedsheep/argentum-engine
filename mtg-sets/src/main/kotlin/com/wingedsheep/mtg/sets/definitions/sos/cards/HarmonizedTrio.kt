package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.model.Rarity

/**
 * Harmonized Trio // Brainstorm — Secrets of Strixhaven #52
 * {U} · Creature — Merfolk Bard Wizard · 1/1
 *
 * {T}, Tap two untapped creatures you control: This creature becomes prepared. (While it's
 * prepared, you may cast a copy of its spell. Doing so unprepares it.)
 * //
 * Brainstorm — {U}, Instant: Draw three cards, then put two cards from your hand on top of your
 * library in any order.
 *
 * Prepare (Secrets of Strixhaven): unlike "enters prepared" creatures, Harmonized Trio has NO
 * PREPARED keyword — it never enters prepared. It only becomes prepared via its activated ability
 * (tap itself plus two other untapped creatures you control) through [Effects.BecomePrepared].
 * Becoming prepared creates a castable copy of "Brainstorm" in exile.
 *
 * Brainstorm composes the atomic draw + put-back pipeline: draw three, then the controller chooses
 * exactly two cards from hand and orders them on top of the library
 * ([com.wingedsheep.sdk.dsl.PipelineBuilder.toLibraryTop] with controller-chosen order).
 */
val HarmonizedTrio = card("Harmonized Trio") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Bard Wizard"
    power = 1
    toughness = 1
    oracleText = "{T}, Tap two untapped creatures you control: This creature becomes prepared. " +
        "(While it's prepared, you may cast a copy of its spell. Doing so unprepares it.)"

    // {T}, Tap two untapped creatures you control: become prepared.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.TapPermanents(2, GameObjectFilter.Creature, excludeSelf = true),
        )
        effect = Effects.BecomePrepared(EffectTarget.Self)
    }

    // Brainstorm — the prepare spell.
    prepare("Brainstorm") {
        manaCost = "{U}"
        typeLine = "Instant"
        oracleText = "Draw three cards, then put two cards from your hand on top of your library " +
            "in any order."
        spell {
            effect = Effects.Composite(
                Effects.DrawCards(3),
                Effects.Pipeline {
                    val hand = gather(CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Any))
                    val putBack = chooseExactly(2, hand)
                    toLibraryTop(putBack)
                },
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "52"
        artist = "Marie Magny"
        imageUri = "https://cards.scryfall.io/normal/front/6/1/617208ff-dd9b-44fd-a740-d3188081e5cc.jpg?1778165069"
    }
}
