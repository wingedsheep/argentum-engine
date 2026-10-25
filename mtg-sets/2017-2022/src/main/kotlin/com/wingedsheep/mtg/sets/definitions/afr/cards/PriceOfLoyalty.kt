package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Price of Loyalty
 * {2}{R}
 * Sorcery
 * Gain control of target creature until end of turn. Untap that creature. It gains haste until end
 * of turn. If mana from a Treasure was spent to cast this spell, that creature gets +2/+0 until end
 * of turn.
 *
 * The Treasure check reads the spell's recorded mana provenance
 * ([DynamicAmounts.manaSpentFromSubtype]) as it resolves, the same way Devour Intellect does.
 */
val PriceOfLoyalty = card("Price of Loyalty") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Gain control of target creature until end of turn. Untap that creature. It gains haste " +
        "until end of turn. If mana from a Treasure was spent to cast this spell, that creature gets " +
        "+2/+0 until end of turn."

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.GrantKeyword(Keyword.HASTE, t) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.manaSpentFromSubtype(Subtype.TREASURE),
                    ComparisonOperator.GTE,
                    1,
                ),
                then = Effects.ModifyStats(2, 0, t, Duration.EndOfTurn),
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "159"
        artist = "Oriana Menendez"
        flavorText = "\"Most folks got mayhem in their hearts. Just gotta know their price.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47634528-cc34-406a-8572-812a41d6a86a.jpg?1783926473"
    }
}
