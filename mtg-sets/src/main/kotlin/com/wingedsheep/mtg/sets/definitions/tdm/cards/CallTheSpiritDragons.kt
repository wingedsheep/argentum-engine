package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Call the Spirit Dragons
 * {W}{U}{B}{R}{G}
 * Enchantment
 *
 * Dragons you control have indestructible.
 * At the beginning of your upkeep, for each color, put a +1/+1 counter on a Dragon you control
 * of that color. If you put +1/+1 counters on five Dragons this way, you win the game.
 *
 * Implementation notes:
 * - "for each color" means each of the five colors (WUBRG), so the upkeep ability is one
 *   resolution-time choice per color rather than a `ForEachColorOf` over the source's colors.
 * - Putting a counter "on a Dragon you control of that color" is a *choice* (no "target"), so it
 *   uses [Effects.SelectTarget] (resolution-time pick), not a declared trigger target. A
 *   multicolored Dragon is a legal choice for every color it is, and may be chosen more than once.
 * - The win condition fires only when five *different* Dragons each received a counter. Each
 *   color's pick is stored under its own pipeline key and the win check counts the *distinct*
 *   entities across all five keys via [DynamicAmounts.distinctEntitiesIn], so picking the same
 *   multicolored Dragon for two colors correctly counts as one Dragon toward the win.
 */
val CallTheSpiritDragons = card("Call the Spirit Dragons") {
    manaCost = "{W}{U}{B}{R}{G}"
    colorIdentity = "WUBRG"
    typeLine = "Enchantment"
    oracleText = "Dragons you control have indestructible.\n" +
        "At the beginning of your upkeep, for each color, put a +1/+1 counter on a Dragon you control " +
        "of that color. If you put +1/+1 counters on five Dragons this way, you win the game."

    // Dragons you control have indestructible.
    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.INDESTRUCTIBLE,
            filter = GroupFilter(GameObjectFilter.Creature.youControl().withSubtype(Subtype.DRAGON))
        )
    }

    triggeredAbility {
        trigger = Triggers.YourUpkeep

        // One choice per color (WUBRG). Each stores its picked Dragon under a distinct key.
        val perColor = listOf(
            Color.WHITE to "spiritDragonW",
            Color.BLUE to "spiritDragonU",
            Color.BLACK to "spiritDragonB",
            Color.RED to "spiritDragonR",
            Color.GREEN to "spiritDragonG"
        )
        val counterSteps = perColor.flatMap { (color, key) ->
            val dragonOfColor = TargetCreature(
                filter = TargetFilter(
                    GameObjectFilter.Creature.youControl().withSubtype(Subtype.DRAGON).withColor(color)
                )
            )
            listOf(
                Effects.SelectTarget(dragonOfColor, key),
                Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.PipelineTarget(key))
            )
        }

        // If five different Dragons received a +1/+1 counter this way, you win the game.
        val winIfFiveDistinct = ConditionalEffect(
            condition = Compare(
                DynamicAmounts.distinctEntitiesIn(*perColor.map { it.second }.toTypedArray()),
                ComparisonOperator.GTE,
                DynamicAmount.Fixed(5)
            ),
            effect = Effects.WinGame(message = "Five spirit Dragons answered the call.")
        )

        effect = Effects.Composite(counterSteps + winIfFiveDistinct)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "174"
        artist = "Liiga Smilshkalne"
        flavorText = "The essence of Tarkir was shaped into draconic embodiments of the re-formed clans."
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b1ad91db-5f16-4392-baf1-f8400ec11e0a.jpg?1743204672"
        ruling("2025-04-04", "If you control a Dragon that is more than one color, you may put more than one +1/+1 counter on it with the last ability of Call the Spirit Dragons.")
        ruling("2025-04-04", "The last ability will only cause you to win the game if you put a +1/+1 counter on each of five different Dragons with it.")
        ruling("2025-04-04", "Because damage remains marked on a creature until the damage is removed as the turn ends, nonlethal damage dealt to a Dragon you control may become lethal if Call the Spirit Dragons leaves the battlefield during that turn.")
    }
}
