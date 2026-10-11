package com.wingedsheep.mtg.sets.definitions.c21.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Perplexing Test — Commander 2021 #30 (the card's earliest printing; reprinted in MOC, WOC, BLC, SOC)
 * {3}{U}{U} · Instant
 *
 * Choose one —
 * • Return all creature tokens to their owners' hands.
 * • Return all nontoken creatures to their owners' hands.
 *
 * Each mode is a group bounce over every player's creatures, split on token-ness.
 */
val PerplexingTest = card("Perplexing Test") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Return all creature tokens to their owners' hands.\n" +
        "• Return all nontoken creatures to their owners' hands."

    spell {
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                effect = Patterns.Group.returnAllToHand(GroupFilter(GameObjectFilter.Creature.token())),
                description = "Return all creature tokens to their owners' hands.",
            ),
            Mode.noTarget(
                effect = Patterns.Group.returnAllToHand(GroupFilter(GameObjectFilter.Creature.nontoken())),
                description = "Return all nontoken creatures to their owners' hands.",
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "Fariba Khamseh"
        flavorText = "\"Your proofs were a bit nontraditional, but effective nonetheless. Full marks.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/1/31f2cbcc-d5b8-4659-ae51-e567c555a743.jpg?1783927602"
        ruling(
            "2021-04-16",
            "A token that is returned to its owner's hand ceases to exist as a state-based action " +
                "the next time a player would receive priority."
        )
    }
}
