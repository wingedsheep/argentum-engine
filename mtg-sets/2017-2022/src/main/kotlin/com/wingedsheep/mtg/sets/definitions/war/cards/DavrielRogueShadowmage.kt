package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Davriel, Rogue Shadowmage — War of the Spark #83 (canonical printing)
 * {2}{B}
 * Legendary Planeswalker — Davriel
 * Loyalty: 3
 *
 * At the beginning of each opponent's upkeep, if that player has one or fewer cards in hand,
 * Davriel deals 2 damage to them.
 * −1: Target player discards a card.
 *
 * The upkeep ability is an intervening-if trigger: hand size is checked when the step begins and
 * again on resolution. Same shape as Hellfire Mongrel.
 */
val DavrielRogueShadowmage = card("Davriel, Rogue Shadowmage") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Planeswalker — Davriel"
    startingLoyalty = 3
    oracleText = "At the beginning of each opponent's upkeep, if that player has one or fewer cards in hand, " +
        "Davriel deals 2 damage to them.\n" +
        "−1: Target player discards a card."

    triggeredAbility {
        trigger = Triggers.anOpponent.beginningOf(Step.UPKEEP)
        // "Them" is the player whose upkeep it is — bound by the step trigger.
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.count(Player.TriggeringPlayer, Zone.HAND),
            ComparisonOperator.LTE,
            1,
        )
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    loyaltyAbility(-1) {
        val t = target(Targets.Player)
        effect = Effects.Discard(1, t)
        description = "Target player discards a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b1a44f0-1db8-406a-b7d9-f883f947f7bd.jpg?1783933448"
    }
}
