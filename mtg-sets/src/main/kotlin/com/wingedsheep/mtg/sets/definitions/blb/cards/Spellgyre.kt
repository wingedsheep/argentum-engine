package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Spellgyre
 * {2}{U}{U}
 * Instant
 *
 * Choose one —
 * - Counter target spell.
 * - Surveil 2, then draw two cards.
 */
val Spellgyre = card("Spellgyre") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one —\n• Counter target spell.\n• Surveil 2, then draw two cards."

    spell {
        effect = ModalEffect.chooseOne(
            Mode.withTarget(
                Effects.CounterSpell(),
                Targets.Spell,
                "Counter target spell"
            ),
            Mode.noTarget(
                LibraryPatterns.surveil(2).then(Effects.DrawCards(2)),
                "Surveil 2, then draw two cards"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "72"
        artist = "Alix Branwyn"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f6f6620a-1d40-429d-9a0c-aaeb62adaa71.jpg?1721426266"
    }
}
