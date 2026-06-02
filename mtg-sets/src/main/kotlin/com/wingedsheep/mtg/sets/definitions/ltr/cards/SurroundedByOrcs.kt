package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Surrounded by Orcs
 * {3}{U}
 * Sorcery
 *
 * Amass Orcs 3, then target player mills X cards, where X is the amassed Army's power.
 *
 * Unlike Foray of Orcs (which uses "When you do" / a reflexive trigger), the "then" wording
 * here means amass and mill resolve as one sequenced effect on a single target chosen at
 * cast time. If the target player becomes illegal before resolution, the whole spell
 * fizzles and amass does not happen (CR 608.2b — Scryfall ruling 2023-06-16: "If each
 * target chosen is an illegal target as that spell or ability tries to resolve, it won't
 * resolve. You won't amass Orcs.").
 */
val SurroundedByOrcs = card("Surrounded by Orcs") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Amass Orcs 3, then target player mills X cards, where X is the amassed Army's power."

    spell {
        val player = target("target player", Targets.Player)
        effect = Effects.Composite(listOf(
            Effects.Amass(3, "Orc"),
            LibraryPatterns.mill(
                DynamicAmount.EntityProperty(EntityReference.AmassedArmy, EntityNumericProperty.Power),
                player
            )
        ))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "73"
        artist = "Anna Pavleeva"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1c50d4c8-2311-4394-b73f-389eb81e898b.jpg?1686968332"
        ruling("2023-06-16", "Some spells and abilities that amass Orcs may require targets. If each target chosen is an illegal target as that spell or ability tries to resolve, it won't resolve. You won't amass Orcs.")
        ruling("2023-06-16", "Some cards refer to the \"amassed Army.\" That means the Army creature you chose to receive counters, even if no counters were placed on it for some reason.")
    }
}
