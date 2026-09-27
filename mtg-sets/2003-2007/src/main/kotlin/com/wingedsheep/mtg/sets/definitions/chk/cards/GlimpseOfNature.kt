package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry

/**
 * Glimpse of Nature
 * {G}
 * Sorcery
 * Whenever you cast a creature spell this turn, draw a card.
 *
 * A turn-bounded, event-based delayed triggered ability: `fireOnce = false` so it fires for
 * *every* creature spell cast this turn, `expiry = EndOfTurn` so it is gone at the turn boundary.
 * Per the ruling, the trigger resolves before (and independently of) the creature spell.
 */
val GlimpseOfNature = card("Glimpse of Nature") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Whenever you cast a creature spell this turn, draw a card."

    spell {
        effect = Effects.CreateDelayedTrigger(
            effect = Effects.DrawCards(1),
            trigger = Triggers.you.casts(GameObjectFilter.Creature),
            fireOnce = false,
            expiry = DelayedTriggerExpiry.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "210"
        artist = "Shishizaru"
        flavorText = "Dosan sat in repose for many hours. He made no motion, no sound at all. And as he sat, nature revealed itself to him."
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1ddcd76b-a7a1-4ae6-bf4a-f929c6574bdc.jpg?1783944290"
        ruling("2026-03-20", "The delayed triggered ability created by Glimpse of Nature resolves before the spell that caused it to trigger. It resolves even if that spell is countered or otherwise leaves the stack.")
    }
}
