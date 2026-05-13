package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Scorpion's Sting
 * {1}{B}
 * Instant
 * "Target creature gets -3/-3 until end of turn."
 */
val ScorpionsSting = card("Scorpion's Sting") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -3/-3 until end of turn."

    spell {
        val creature = target("target creature", Targets.Creature)
        effect = Effects.ModifyStats(-3, -3, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "65"
        artist = "Lee Woo-chul"
        flavorText = "\"I'm going to kill you, bug.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0fb03437-32cf-4c97-bf91-ea8b2ad3f964.jpg?1757377164"
    }
}
