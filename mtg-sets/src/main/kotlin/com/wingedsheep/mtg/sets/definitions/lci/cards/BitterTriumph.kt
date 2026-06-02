package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreatureOrPlaneswalker
import com.wingedsheep.sdk.dsl.Costs

/**
 * Bitter Triumph
 * {1}{B}
 * Instant
 *
 * As an additional cost to cast this spell, discard a card or pay 3 life.
 * Destroy target creature or planeswalker.
 *
 * Modeled with [ModalEffect] for the binary cost fork (discard a card or
 * pay 3 life), matching Feed the Cycle's "forage or pay {B}" pattern.
 * The spell is NOT modal in MTG terms — there is no "Choose one — • X • Y"
 * wording. `countsAsModalSpell = false` keeps Riku of Many Paths and
 * other "Whenever you cast a modal spell" triggers from misreading it.
 */
val BitterTriumph = card("Bitter Triumph") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "As an additional cost to cast this spell, discard a card or pay 3 life.\nDestroy target creature or planeswalker."

    spell {
        effect = ModalEffect.chooseOne(
            // Mode 1: Discard a card
            Mode(
                effect = Effects.Destroy(EffectTarget.ContextTarget(0)),
                targetRequirements = listOf(TargetCreatureOrPlaneswalker()),
                description = "Discard a card — destroy target creature or planeswalker",
                additionalCosts = listOf(Costs.additional.DiscardCards(count = 1))
            ),
            // Mode 2: Pay 3 life
            Mode(
                effect = Effects.Destroy(EffectTarget.ContextTarget(0)),
                targetRequirements = listOf(TargetCreatureOrPlaneswalker()),
                description = "Pay 3 life — destroy target creature or planeswalker",
                additionalCosts = listOf(Costs.additional.PayLife(amount = 3))
            ),
            countsAsModalSpell = false
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "91"
        artist = "Donato Giancola"
        flavorText = "Though he wept at Inti's death, Caparocti's eyes were keen, and his spear pierced cleanly through Clavileño's tainted heart."
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05bdd22c-3e11-4c29-bdfa-d3dfc0e90a9f.jpg?1699044085"
    }
}
