package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rust — Legends #203
 * {G} · Instant
 *
 * Counter target activated ability from an artifact source.
 * (Mana abilities can't be targeted.)
 *
 * Same target/effect shape as Brown Ouphe's ability, as a spell.
 */
val Rust = card("Rust") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Counter target activated ability from an artifact source. " +
        "(Mana abilities can't be targeted.)"

    spell {
        target(TargetFilter.ActivatedAbilityOnStack.abilitySourceMatches(GameObjectFilter.Artifact))
        effect = Effects.CounterAbility()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "203"
        artist = "Liz Danforth"
        flavorText = "\"How dull it is to pause, to make an end,/ To rust unburnished, not to shine in use,/ " +
            "As though to breathe were life!\"\n—Alfred, Lord Tennyson, \"Ulysses\""
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad4974c8-34c5-4290-b325-7586a67f6d56.jpg?1783948044"
    }
}
