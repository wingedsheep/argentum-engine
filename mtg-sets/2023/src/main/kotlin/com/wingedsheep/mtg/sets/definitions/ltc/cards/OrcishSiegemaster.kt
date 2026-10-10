package com.wingedsheep.mtg.sets.definitions.ltc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Orcish Siegemaster
 * {2}{R}
 * Creature — Orc Soldier
 * 0/5
 *
 * Trample
 * Other Orcs and Goblins you control have trample.
 * Whenever this creature attacks, it gets +X/+0 until end of turn, where X is the greatest power
 * among creatures you control.
 *
 * X is computed once, as the trigger resolves (Siegemaster itself counts), and the bonus is then
 * locked in — `ModifyStatsExecutor` evaluates the amount at resolution.
 */
val OrcishSiegemaster = card("Orcish Siegemaster") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc Soldier"
    power = 0
    toughness = 5
    oracleText = "Trample\n" +
        "Other Orcs and Goblins you control have trample.\n" +
        "Whenever this creature attacks, it gets +X/+0 until end of turn, where X is the greatest power among creatures you control."

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.TRAMPLE,
            filter = GroupFilter(
                GameObjectFilter.Creature.withAnySubtype("Orc", "Goblin").youControl(),
                excludeSelf = true
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ModifyStats(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower(),
            DynamicAmounts.fixed(0),
            EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Anton Solovianchyk"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e9f9869-0f43-4eac-892f-26cd8a614330.jpg?1783916028"
    }
}
