package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Hold the Gates — Gatecrash #16
 * {2}{W} · Enchantment
 *
 * Creatures you control get +0/+1 for each Gate you control and have vigilance.
 *
 * The toughness bonus is a live count of Gates you control, so it grows and shrinks as Gates
 * enter and leave.
 */
val HoldTheGates = card("Hold the Gates") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Creatures you control get +0/+1 for each Gate you control and have vigilance."

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter(GameObjectFilter.Creature.youControl()),
            powerBonus = DynamicAmounts.fixed(0),
            toughnessBonus = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Land.withSubtype(Subtype.GATE.value),
            ).count(),
        )
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter(GameObjectFilter.Creature.youControl()))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "16"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48fd52d0-0e41-48d5-b96f-4c6409788c18.jpg?1783940143"
    }
}
