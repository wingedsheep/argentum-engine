package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Limits of Solidarity
 * {3}{R}
 * Sorcery
 * Gain control of target creature until end of turn. Untap that creature. It gains haste until end of turn.
 * Cycling {2}
 */
val LimitsOfSolidarity = card("Limits of Solidarity") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature until end of turn. Untap that creature. It gains haste until end of turn.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.GrantKeyword(Keyword.HASTE, t)
    }

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "140"
        artist = "Craig J Spearing"
        flavorText = "\"When the time comes, only one of you will be honored by my spear.\"\n—Hazoret, god of zeal"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/95cb6a18-1222-4e56-8466-0f7bd634ef3d.jpg?1783936485"
    }
}
