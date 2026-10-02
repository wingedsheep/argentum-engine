package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Marauding Knight
 * {2}{B}{B}
 * Creature — Phyrexian Zombie Knight
 * 2/2
 * Protection from white
 * This creature gets +1/+1 for each Plains your opponents control.
 */
val MaraudingKnight = card("Marauding Knight") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Zombie Knight"
    power = 2
    toughness = 2
    oracleText = "Protection from white\nThis creature gets +1/+1 for each Plains your opponents control."

    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.WHITE)))

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = DynamicAmounts.battlefield(Player.EachOpponent, GameObjectFilter.Land.withSubtype("Plains")).count(),
            toughnessBonus = DynamicAmounts.battlefield(Player.EachOpponent, GameObjectFilter.Land.withSubtype("Plains")).count()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "110"
        artist = "Daren Bader"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/cea2a7de-c67e-4541-be8c-e5ef7b64d94a.jpg?1562936560"
    }
}
