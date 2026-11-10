package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Immolating Glare
 * {1}{W}
 * Instant
 * Destroy target attacking creature.
 */
val ImmolatingGlare = card("Immolating Glare") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target attacking creature."

    spell {
        val victim = target(TargetFilter.AttackingCreature)
        effect = Effects.Destroy(victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "20"
        artist = "Daarken"
        flavorText = "\"The sun can warm your skin or blister it, nurture your crops or wither them, light your path " +
            "or blind you. She is a fickle friend, but a far worse enemy.\"\n—Nazra of the Stone Havens"
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f468338-bb66-4db0-a883-69095566092b.jpg"
    }
}
