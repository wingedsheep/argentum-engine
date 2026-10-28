package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Merfolk Windrobber — Zendikar Rising #70
 * {U} · Creature — Merfolk Rogue · 1/1
 *
 * Flying
 * Whenever this creature deals combat damage to a player, that player mills a card.
 * Sacrifice this creature: Draw a card. Activate only if an opponent has eight or more cards in
 * their graveyard.
 *
 * The gate is Expedition Lookout's per-opponent test: the greatest graveyard among opponents
 * (any single opponent at eight or more), not a sum across opponents.
 */
val MerfolkWindrobber = card("Merfolk Windrobber") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Rogue"
    oracleText = "Flying\n" +
        "Whenever this creature deals combat damage to a player, that player mills a card. " +
        "(They put the top card of their library into their graveyard.)\n" +
        "Sacrifice this creature: Draw a card. Activate only if an opponent has eight or more cards " +
        "in their graveyard."
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Patterns.Library.mill(1, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    activatedAbility {
        cost = Costs.SacrificeSelf
        effect = Effects.DrawCards(1)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.CompareAmounts(
                    DynamicAmounts.greatestAmongPlayers(
                        DynamicAmounts.count(Player.You, Zone.GRAVEYARD),
                        Player.EachOpponent
                    ),
                    ComparisonOperator.GTE,
                    8
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "70"
        artist = "Colin Boyer"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/112077b8-1514-4320-a70f-b23f3c7ce18a.jpg?1783929392"
        ruling(
            "2020-09-25",
            "An ability that offers a bonus if an opponent has eight or more cards in their graveyard " +
                "won't provide additional benefits if more than one opponent has eight or more cards in " +
                "their graveyard. There are also no additional benefits no matter how many cards are in " +
                "an opponent's graveyard, as long as there are at least eight."
        )
    }
}
