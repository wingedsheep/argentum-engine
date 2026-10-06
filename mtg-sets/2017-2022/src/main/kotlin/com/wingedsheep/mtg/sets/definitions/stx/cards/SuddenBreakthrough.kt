package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sudden Breakthrough — Strixhaven: School of Mages #116 (canonical printing)
 * {1}{R}
 * Instant
 * Target creature gets +2/+0 and gains first strike until end of turn.
 * Create a Treasure token.
 */
val SuddenBreakthrough = card("Sudden Breakthrough") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+0 and gains first strike until end of turn.\n" +
        "Create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 0, t) then
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, t) then
            Effects.CreateTreasure()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "116"
        artist = "Colin Boyer"
        flavorText = "Rootha created constantly, passionately, in hopes of impressing her greatest critic: herself."
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b011707a-471b-41c2-bc73-376a55d26bee.jpg?1783927349"
    }
}
