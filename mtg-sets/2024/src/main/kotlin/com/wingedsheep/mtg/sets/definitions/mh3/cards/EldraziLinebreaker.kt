package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Eldrazi Linebreaker
 * {1}{C}{R}
 * Creature — Eldrazi
 * 3/3
 *
 * Devoid
 * Trample
 * At the beginning of combat on your turn, target creature you control gains haste and gets +X/+0
 * until end of turn, where X is the number of Eldrazi you control.
 *
 * "Eldrazi" is a bare tribal noun, so X counts every Eldrazi *permanent* you control, not only
 * creatures. X is fixed as the trigger resolves (CR 608.2h).
 */
val EldraziLinebreaker = card("Eldrazi Linebreaker") {
    manaCost = "{1}{C}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Eldrazi"
    power = 3
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "Trample\n" +
        "At the beginning of combat on your turn, target creature you control gains haste and gets " +
        "+X/+0 until end of turn, where X is the number of Eldrazi you control."

    keywords(Keyword.DEVOID, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        val t = target(TargetFilter.CreatureYouControl)
        val eldrazi = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Permanent.withSubtype("Eldrazi")
        ).count()
        effect = Effects.GrantKeyword(Keyword.HASTE, t) then
            Effects.ModifyStats(power = eldrazi, toughness = DynamicAmounts.fixed(0), target = t)
        description = "At the beginning of combat on your turn, target creature you control gains " +
            "haste and gets +X/+0 until end of turn, where X is the number of Eldrazi you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "117"
        artist = "Leonardo Santanna"
        imageUri = "https://cards.scryfall.io/normal/front/f/6/f67774a1-f5f8-4b7b-871d-88a1b5e57d27.jpg?1783911273"
    }
}
