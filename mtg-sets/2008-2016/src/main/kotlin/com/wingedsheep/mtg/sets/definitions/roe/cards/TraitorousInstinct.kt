package com.wingedsheep.mtg.sets.definitions.roe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Traitorous Instinct (Rise of the Eldrazi #168)
 * {3}{R}
 * Sorcery
 *
 * Gain control of target creature until end of turn. Untap that creature. Until end of turn, it gets
 * +2/+0 and gains haste.
 */
val TraitorousInstinct = card("Traitorous Instinct") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature until end of turn. Untap that creature. Until end of turn, " +
        "it gets +2/+0 and gains haste."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.ModifyStats(2, 0, t, Duration.EndOfTurn) then
            Effects.GrantKeyword(Keyword.HASTE, t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "168"
        artist = "Scott Chou"
        flavorText = "The horror lurched about like a puppet from a madman's nightmare, squeezing the life " +
            "from its own kind."
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d65b63ea-e3c3-465d-8cd9-7251cda9cc63.jpg?1783941970"
        ruling(
            "2010-06-15",
            "You may target a creature you already control. You may target a creature that's already untapped.",
        )
    }
}
