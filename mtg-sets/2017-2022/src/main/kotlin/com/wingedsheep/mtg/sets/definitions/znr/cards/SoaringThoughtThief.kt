package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Soaring Thought-Thief {U}{B} — Zendikar Rising #236
 * Creature — Human Rogue 1/3
 *
 * "An opponent has eight or more cards in their graveyard" is Blackbloom Rogue's per-opponent test
 * (greatest graveyard among opponents, not a sum). The lord bonus includes this creature itself.
 * "One or more Rogues you control attack" is the once-per-declaration [Triggers.you.attacks]; every
 * opponent mills, not only the defending ones (ruling).
 */
val SoaringThoughtThief = card("Soaring Thought-Thief") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Creature — Human Rogue"
    oracleText = "Flash\nFlying\n" +
        "As long as an opponent has eight or more cards in their graveyard, Rogues you control get +1/+0.\n" +
        "Whenever one or more Rogues you control attack, each opponent mills two cards."
    power = 1
    toughness = 3

    keywords(Keyword.FLASH, Keyword.FLYING)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(
                powerBonus = 1,
                toughnessBonus = 0,
                filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Rogue").youControl())
            ),
            condition = Conditions.CompareAmounts(
                DynamicAmounts.greatestAmongPlayers(
                    DynamicAmounts.count(Player.You, Zone.GRAVEYARD),
                    Player.EachOpponent
                ),
                ComparisonOperator.GTE,
                8
            )
        )
    }

    triggeredAbility {
        trigger = Triggers.you.attacks(
            with = GameObjectFilter.Creature.withSubtype("Rogue").youControl()
        )
        effect = Patterns.Library.mill(2, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Whenever one or more Rogues you control attack, each opponent mills two cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "236"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/0/d/0d73c9c4-8955-499f-8f87-6500b35b4977.jpg?1783929318"
        ruling("2020-09-25", "Soaring Thought-Thief's third ability applies to itself as well as other Rogues you control.")
        ruling(
            "2020-09-25",
            "Soaring Thought-Thief's fourth ability causes each opponent to mill two cards, not just those " +
                "being attacked by Rogues you control."
        )
        ruling(
            "2020-09-25",
            "An ability that offers a bonus if an opponent has eight or more cards in their graveyard " +
                "won't provide additional benefits if more than one opponent has eight or more cards in " +
                "their graveyard. There are also no additional benefits no matter how many cards are in " +
                "an opponent's graveyard, as long as there are at least eight."
        )
        ruling(
            "2020-09-25",
            "In a multiplayer game, once an opponent leaves the game, they won't be considered for such " +
                "an effect, no matter how many cards they had in their graveyard before leaving the game."
        )
    }
}
