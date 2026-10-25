package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Skeletal Swarming
 * {3}{B}{G}
 * Enchantment
 *
 * Each Skeleton you control has trample, attacks each combat if able, and gets +X/+0, where X is
 * the number of other Skeletons you control.
 * At the beginning of your end step, create a tapped 1/1 black Skeleton creature token. If a
 * creature died this turn, create two of those tokens instead.
 *
 * "Other" is relative to each Skeleton receiving the bonus: inside a static's dynamic amount,
 * `excludeSelf` resolves to the affected entity, so every Skeleton counts all Skeletons you
 * control except itself. "A creature died this turn" is checked on resolution (not an
 * intervening-if), so the ability always triggers and makes one or two tokens.
 */
val SkeletalSwarming = card("Skeletal Swarming") {
    manaCost = "{3}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Enchantment"
    oracleText = "Each Skeleton you control has trample, attacks each combat if able, and gets +X/+0, " +
        "where X is the number of other Skeletons you control.\n" +
        "At the beginning of your end step, create a tapped 1/1 black Skeleton creature token. " +
        "If a creature died this turn, create two of those tokens instead."

    val skeletons = GameObjectFilter.Creature.withSubtype("Skeleton")
    val yourSkeletons = GroupFilter(skeletons.youControl())

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE, yourSkeletons)
    }

    staticAbility {
        ability = MustAttack(yourSkeletons)
    }

    staticAbility {
        val otherSkeletons = DynamicAmounts.battlefield(Player.You, skeletons, excludeSelf = true).count()
        ability = GrantDynamicStats(
            filter = yourSkeletons,
            powerBonus = otherSkeletons,
            toughnessBonus = DynamicAmounts.fixed(0)
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.CreateToken(
            count = DynamicAmounts.conditional(Conditions.CreatureDiedThisTurn, 2, 1),
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Skeleton"),
            tapped = true,
            imageUri = "https://cards.scryfall.io/normal/front/f/a/fa6fdb57-82f3-4695-b1fa-1f301ea4ef83.jpg?1783926346"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "232"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/3768ac68-0d87-4f99-aa77-2d009df0a567.jpg?1783926443"
    }
}
