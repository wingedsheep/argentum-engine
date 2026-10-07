package com.wingedsheep.mtg.sets.definitions.m10.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

val ActOfTreason = card("Act of Treason") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature until end of turn. Untap that creature. It gains haste until end of turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.GrantKeyword(Keyword.HASTE, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "124"
        artist = "Eric Deschamps"
        flavorText = "\"Rage courses in every heart, yearning to betray its rational prison.\"\n—Sarkhan Vol"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b63bee5-d8e5-4c2f-8514-8c86d025f7c9.jpg?1783942376"
    }
}
