package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Root Out — Shadows over Innistrad #224
 * {2}{G} · Sorcery
 *
 * Destroy target artifact or enchantment.
 * Investigate.
 */
val RootOut = card("Root Out") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Destroy target artifact or enchantment.\nInvestigate. " +
        "(Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        val t = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.Destroy(t) then Effects.Investigate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "224"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ffb04258-d6ab-440c-85d2-05a995aeba2c.jpg?1783937722"
        ruling(
            "2016-04-08",
            "You can't cast a spell without choosing legal targets. If all of those targets become " +
                "illegal, the spell doesn't resolve and you won't investigate.",
        )
    }
}
