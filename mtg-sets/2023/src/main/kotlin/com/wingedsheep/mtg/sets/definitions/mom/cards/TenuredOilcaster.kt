package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tenured Oilcaster
 * {3}{B}
 * Creature — Phyrexian Wizard
 * 2/4
 * Menace
 * This creature gets +3/+0 as long as an opponent has eight or more cards in their graveyard.
 * Whenever this creature attacks or blocks, each player mills a card.
 *
 * "An opponent has eight or more" is per-opponent (the Expedition Lookout shape): the gate is the
 * greatest graveyard size among opponents, not their summed total. "Attacks or blocks" is two
 * triggered abilities sharing one effect (the Merfolk Skyscout shape).
 */
private val OpponentHasEightInGraveyard = Conditions.CompareAmounts(
    DynamicAmounts.greatestAmongPlayers(
        DynamicAmounts.count(Player.You, Zone.GRAVEYARD),
        Player.EachOpponent
    ),
    ComparisonOperator.GTE,
    8
)

val TenuredOilcaster = card("Tenured Oilcaster") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Wizard"
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "This creature gets +3/+0 as long as an opponent has eight or more cards in their graveyard.\n" +
        "Whenever this creature attacks or blocks, each player mills a card."
    power = 2
    toughness = 4

    keywords(Keyword.MENACE)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 3, toughnessBonus = 0, filter = GroupFilter.source()),
            condition = OpponentHasEightInGraveyard
        )
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Library.mill(1, EffectTarget.PlayerRef(Player.Each))
    }

    triggeredAbility {
        trigger = Triggers.self.blocks()
        effect = Patterns.Library.mill(1, EffectTarget.PlayerRef(Player.Each))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "Francisco Miyara"
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65721bc1-87fa-45b9-8b45-ee77c1aab6ac.jpg?1783916999"
    }
}
