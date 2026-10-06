package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Declare Dominance
 * {3}{G}{G}
 * Sorcery
 * Target creature gets +3/+3 until end of turn. All creatures able to block it this turn do so.
 */
val DeclareDominance = card("Declare Dominance") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Target creature gets +3/+3 until end of turn. All creatures able to block it this turn do so."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(3, 3, t) then Effects.MustBeBlocked(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "Simon Dominic"
        flavorText = "\"If you think yourself a chief, then prove your might.\"\n—Gar-Tun, Mistvalley silverback"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/78271f34-f62e-4771-b430-b121097b8fb6.jpg?1783934538"
    }
}
