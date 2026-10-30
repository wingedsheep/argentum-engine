package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Enemy of Enlightenment
 * {5}{B}
 * Enchantment Creature — Demon
 * 5/5
 * Flying
 * This creature gets -1/-1 for each card in your opponents' hands.
 * At the beginning of your upkeep, each player discards a card.
 *
 * The shrink is Stingerback Terror's self-scoped [GrantDynamicStats], counting every opponent's
 * hand instead of yours. The upkeep trigger is Liliana of the Veil's [Effects.EachPlayerDiscards].
 */
val EnemyOfEnlightenment = card("Enemy of Enlightenment") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment Creature — Demon"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "This creature gets -1/-1 for each card in your opponents' hands.\n" +
        "At the beginning of your upkeep, each player discards a card."

    keywords(Keyword.FLYING)

    staticAbility {
        val negOpponentHands = -DynamicAmounts.count(Player.EachOpponent, Zone.HAND)
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = negOpponentHands,
            toughnessBonus = negOpponentHands
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.EachPlayerDiscards(1)
        description = "At the beginning of your upkeep, each player discards a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "James Ryman"
        flavorText = "\"Evil flourishes where ignorance thrives.\"\n—Perisophia the philosopher"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28bf15e0-f61b-43b9-9113-39bd341a0163.jpg?1783931569"
        ruling(
            "2020-01-24",
            "If a spell or ability causes an opponent to draw one or more cards, wait until that spell or " +
                "ability has finished resolving to determine whether Enemy of Enlightenment dies for having 0 " +
                "toughness. If that spell or ability also causes cards to leave that player's hand, Enemy of " +
                "Enlightenment may briefly be a 0/0 creature without dying."
        )
        ruling("2020-01-24", "If any player can't discard a card, skip that player and each other player discards a card.")
    }
}
