package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Portent of Betrayal
 * {3}{R}
 * Sorcery
 * Gain control of target creature until end of turn. Untap that creature. It gains haste until end
 * of turn. Scry 1.
 *
 * Act of Treason's threaten chain followed by Voyage's End's trailing scry. The scry rides on the
 * single target: if the creature is an illegal target on resolution, the spell doesn't resolve.
 */
val PortentOfBetrayal = card("Portent of Betrayal") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature until end of turn. Untap that creature. It gains haste " +
        "until end of turn. Scry 1. (Look at the top card of your library. You may put that card on the bottom.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.GainControl(creature, Duration.EndOfTurn) then
            Effects.Untap(creature) then
            Effects.GrantKeyword(Keyword.HASTE, creature) then
            Effects.Scry(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/9/f/9fcdf4df-1900-427b-998f-957754ce1b75.jpg?1783939757"
        ruling(
            "2020-11-10",
            "Portent of Betrayal can target any creature, even one that's untapped or one you already control."
        )
        ruling(
            "2020-11-10",
            "If the target creature is an illegal target by the time Portent of Betrayal tries to resolve, " +
                "the spell won't resolve. You won't scry 1."
        )
    }
}
