package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Arrester's Zeal
 * {W}
 * Instant
 * Target creature gets +2/+2 until end of turn.
 * Addendum — If you cast this spell during your main phase, that creature gains flying until end of turn.
 *
 * Addendum is checked at resolution: [Conditions.WasCast] excludes copies (never cast), and
 * [Conditions.IsYourMainPhase] reads the cast-time phase, since a phase only ends with an empty stack.
 */
val ArrestersZeal = card("Arrester's Zeal") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn.\n" +
        "Addendum — If you cast this spell during your main phase, that creature gains flying until end of turn."

    spell {
        val creature = target(TargetFilter.Creature)

        effect = Effects.ModifyStats(2, 2, creature) then Effects.If(
            condition = Conditions.All(Conditions.WasCast, Conditions.IsYourMainPhase),
            then = Effects.GrantKeyword(Keyword.FLYING, creature),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "4"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b075a6f2-5196-45e9-b062-f131f7b1a347.jpg?1783933726"
    }
}
