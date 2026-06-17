package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Elite Interceptor // Rejoinder — Secrets of Strixhaven #12
 * {W} · Creature — Human Wizard · 1/2
 *
 * This creature enters prepared. (While it's prepared, you may cast a copy of its spell.
 * Doing so unprepares it.)
 * //
 * Rejoinder — {1}{W}, Sorcery: You may tap or untap target creature. Draw a card.
 *
 * Prepare (Secrets of Strixhaven): the creature enters with the PREPARED keyword. Becoming
 * prepared creates a copy of its prepare spell ("Rejoinder") in exile that its controller may
 * cast for {1}{W}; casting that copy unprepares the creature. Modeled via
 * [com.wingedsheep.sdk.model.CardLayout.PREPARE] + the `prepare(name) { }` DSL.
 *
 * "You may tap or untap target creature" — the target is chosen at cast time; at resolution the
 * controller may decline ([MayEffect]) or choose one of two modes ([ModalEffect.chooseOne]) that
 * both act on the already-chosen target ([EffectTarget.ContextTarget]). The draw is unconditional,
 * outside the may-clause.
 */
val EliteInterceptor = card("Elite Interceptor") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 2
    oracleText = "This creature enters prepared. (While it's prepared, you may cast a copy of its spell. Doing so unprepares it.)"

    keywords(Keyword.PREPARED)

    // Rejoinder — the prepare spell. You may tap or untap target creature, then draw a card.
    prepare("Rejoinder") {
        manaCost = "{1}{W}"
        typeLine = "Sorcery"
        oracleText = "You may tap or untap target creature.\nDraw a card."
        spell {
            target = Targets.Creature
            effect = Effects.Composite(
                MayEffect(
                    ModalEffect.chooseOne(
                        Mode.noTarget(
                            Effects.Tap(EffectTarget.ContextTarget(0)),
                            "Tap that creature"
                        ),
                        Mode.noTarget(
                            Effects.Untap(EffectTarget.ContextTarget(0)),
                            "Untap that creature"
                        ),
                        countsAsModalSpell = false
                    ),
                    descriptionOverride = "You may tap or untap target creature."
                ),
                Effects.DrawCards(1)
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "12"
        artist = "Lindsey Look"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/2970683e-e69c-42cb-a067-34abd56fb42b.jpg?1775936992"
    }
}
