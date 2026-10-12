package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Keeper of the Cadence
 * {4}{U}
 * Creature — Human Wizard
 * 2/5
 * {3}: Put target artifact, instant, or sorcery card from a graveyard on the bottom of its
 * owner's library.
 *
 * Any graveyard (not just yours); the card goes to its *owner's* library, which
 * [Effects.PutOnBottomOfLibrary] does by construction (Sundering Archaic's shape).
 */
val KeeperOfTheCadence = card("Keeper of the Cadence") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 5
    oracleText = "{3}: Put target artifact, instant, or sorcery card from a graveyard on the bottom " +
        "of its owner's library."

    activatedAbility {
        cost = Costs.Mana("{3}")
        val graveyardCard = target(
            TargetFilter(GameObjectFilter.Artifact or GameObjectFilter.InstantOrSorcery, zone = Zone.GRAVEYARD)
        )
        effect = Effects.PutOnBottomOfLibrary(graveyardCard)
        description = "Put target artifact, instant, or sorcery card from a graveyard on the bottom " +
            "of its owner's library."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Alix Branwyn"
        flavorText = "\"What you call magic, I call the power of memory.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/9/09200388-47c9-4472-ac9e-98f57653e084.jpg?1783920111"
    }
}
