package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Skillful Lunge
 * {1}{W}
 * Instant
 * Target creature gets +2/+0 and gains first strike until end of turn.
 */
val SkillfulLunge = card("Skillful Lunge") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+0 and gains first strike until end of turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 0, creature) then Effects.GrantKeyword(Keyword.FIRST_STRIKE, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Jason Felix"
        flavorText = "\"Valor is a currency of the living that serves us well.\"\n—Cosper Lowe of the Silbern Guard"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2a28abc1-3e75-4db4-baa1-b47abdb7453b.jpg"
    }
}
