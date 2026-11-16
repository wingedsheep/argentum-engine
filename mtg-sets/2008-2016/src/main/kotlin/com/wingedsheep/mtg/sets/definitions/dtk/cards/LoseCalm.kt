package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Lose Calm
 * {3}{R}
 * Sorcery
 * Gain control of target creature until end of turn. Untap that creature. It gains haste and
 * menace until end of turn. (A creature with menace can't be blocked except by two or more
 * creatures.)
 */
val LoseCalm = card("Lose Calm") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature until end of turn. Untap that creature. " +
        "It gains haste and menace until end of turn. " +
        "(A creature with menace can't be blocked except by two or more creatures.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.GrantKeyword(Keyword.HASTE, t) then
            Effects.GrantKeyword(Keyword.MENACE, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "147"
        artist = "Jason A. Engle"
        flavorText = "A lifetime of discipline forsaken in a moment of rage."
        imageUri = "https://cards.scryfall.io/normal/front/7/3/732a4912-97d1-4640-976e-ae1ffeb70236.jpg?1783938588"
    }
}
