package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * King Darien XLVIII
 * {1}{G}{W}
 * Legendary Creature — Human Soldier
 * 2/3
 *
 * Other creatures you control get +1/+1.
 * {3}{G}{W}: Put a +1/+1 counter on King Darien and create a 1/1 white Soldier creature token.
 * Sacrifice King Darien: Creature tokens you control gain hexproof and indestructible until end of turn.
 */
val KingDarienXLVIII = card("King Darien XLVIII") {
    manaCost = "{1}{G}{W}"
    typeLine = "Legendary Creature — Human Soldier"
    power = 2
    toughness = 3
    oracleText = "Other creatures you control get +1/+1.\n{3}{G}{W}: Put a +1/+1 counter on King Darien and create a 1/1 white Soldier creature token.\nSacrifice King Darien: Creature tokens you control gain hexproof and indestructible until end of turn."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true),
        )
    }

    activatedAbility {
        cost = Costs.Mana("{3}{G}{W}")
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Soldier"),
            )
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Patterns.Group.grantKeywordToAll(
            keyword = Keyword.HEXPROOF,
            filter = GroupFilter(GameObjectFilter.Creature.token().youControl()),
            duration = Duration.EndOfTurn,
        ) then Patterns.Group.grantKeywordToAll(
            keyword = Keyword.INDESTRUCTIBLE,
            filter = GroupFilter(GameObjectFilter.Creature.token().youControl()),
            duration = Duration.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "204"
        artist = "Donato Giancola"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/10eca3b1-f35a-4984-bfc0-818e619b1ece.jpg?1783921282"
        ruling("2022-09-09", "If King Darien XLVIII leaves the battlefield, other creatures you control immediately lose the +1/+1 bonus. Notably, this means that if you activate its last ability after a creature has already been dealt damage, it might die due to having lethal damage before the ability resolves and gives it indestructible.")
    }
}
