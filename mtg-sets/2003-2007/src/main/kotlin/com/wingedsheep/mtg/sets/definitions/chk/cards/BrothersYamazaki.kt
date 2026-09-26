package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.LegendRuleDoesNotApplyTo
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Brothers Yamazaki
 * {2}{R}
 * Legendary Creature — Human Samurai
 * 2/1
 * Bushido 1 (Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.)
 * If there are exactly two permanents named Brothers Yamazaki on the battlefield, the "legend rule"
 * doesn't apply to them.
 * Each other creature named Brothers Yamazaki gets +2/+2 and has haste.
 *
 * The exemption is `LegendRuleDoesNotApplyTo` behind a `ConditionalStaticAbility` counting the
 * same-named permanents on the whole battlefield; `LegendRuleCheck` re-evaluates it at every SBA
 * check, so a third copy switches it off and the controller keeps one.
 */
val BrothersYamazaki = card("Brothers Yamazaki") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Samurai"
    power = 2
    toughness = 1
    oracleText = "Bushido 1 (Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.)\n" +
        "If there are exactly two permanents named Brothers Yamazaki on the battlefield, the \"legend rule\" doesn't apply to them.\n" +
        "Each other creature named Brothers Yamazaki gets +2/+2 and has haste."

    keywordAbility(KeywordAbility.bushido(1))

    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        description = "Bushido 1"
    }

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked()
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        description = "Bushido 1"
    }

    staticAbility {
        val brothers = GameObjectFilter.Permanent.named("Brothers Yamazaki")
        ability = ConditionalStaticAbility(
            ability = LegendRuleDoesNotApplyTo(brothers),
            condition = Conditions.CompareAmounts(
                DynamicAmounts.battlefield(Player.Each, brothers).count(),
                ComparisonOperator.EQ,
                2
            )
        )
    }

    staticAbility {
        ability = ModifyStats(2, 2, GroupFilter(GameObjectFilter.Creature.named("Brothers Yamazaki")).other())
    }

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE, GroupFilter(GameObjectFilter.Creature.named("Brothers Yamazaki")).other())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160a"
        artist = "Ron Spears"
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acef8c94-469b-4a76-b507-25b51f2501ab.jpg?1783944303"
    }
}
