package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Veil of Summer
 * {G}
 * Instant
 *
 * Draw a card if an opponent has cast a blue or black spell this turn. Spells you control can't
 * be countered this turn. You and permanents you control gain hexproof from blue and from black
 * until end of turn.
 */
val VeilOfSummer = card("Veil of Summer") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Draw a card if an opponent has cast a blue or black spell this turn. Spells you control " +
        "can't be countered this turn. You and permanents you control gain hexproof from blue and from black " +
        "until end of turn. (You and they can't be the targets of blue or black spells or abilities your " +
        "opponents control.)"

    spell {
        val blueOrBlack = ProtectionScope.Colors(setOf(Color.BLUE, Color.BLACK))
        effect = Effects.If(
            Conditions.CompareAmounts(
                DynamicAmounts.spellsCastThisTurn(
                    player = Player.EachOpponent,
                    filter = GameObjectFilter.Any.withAnyColor(Color.BLUE, Color.BLACK)
                ),
                ComparisonOperator.GTE,
                1
            ),
            Effects.DrawCards(1)
        ) then
            Effects.GrantSpellsCantBeCountered(spellFilter = GameObjectFilter.Any) then
            Effects.GrantHexproofFrom(blueOrBlack) then
            Effects.ForEachInGroup(
                Filters.Group.permanentsYouControl,
                Effects.GrantHexproofFrom(blueOrBlack, EffectTarget.IterationEntity)
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "198"
        artist = "Lake Hurwitz"
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa686c34-1c11-469f-93c2-f9891aea521f.jpg?1783932955"

        ruling("2019-07-12", "If your opponents have cast more than one blue or black spell, you still draw only one card as Veil of Summer resolves.")
        ruling("2019-07-12", "Veil of Summer has no effect until it resolves. It can be countered.")
        ruling("2019-07-12", "A spell or ability that counters spells can still target your spells after Veil of Summer resolves. When that spell or ability resolves, your spell won't be countered, but any additional effects of the countering spell or ability will still happen.")
    }
}
