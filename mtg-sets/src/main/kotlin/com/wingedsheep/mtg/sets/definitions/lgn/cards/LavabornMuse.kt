package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Lavaborn Muse
 * {3}{R}
 * Creature — Spirit
 * 3/3
 * At the beginning of each opponent's upkeep, if that player has two or fewer
 * cards in hand, Lavaborn Muse deals 3 damage to that player.
 *
 * Intervening-if: The hand size is checked both when the trigger fires and when it resolves.
 */
val LavabornMuse = card("Lavaborn Muse") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Spirit"
    oracleText = "At the beginning of each opponent's upkeep, if that player has two or fewer cards in hand, Lavaborn Muse deals 3 damage to that player."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.EachOpponentUpkeep
        // "That player" is the player whose upkeep it is — bound by the step trigger.
        triggerCondition = upkeepPlayerHandAtMost(2)
        effect = ConditionalEffect(
            condition = upkeepPlayerHandAtMost(2),
            effect = Effects.DealDamage(3, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "105"
        artist = "Brian Snõddy"
        flavorText = "Her voice is disaster, painful and final. —Matoc, lavamancer"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4cbc94fb-9e3f-4075-bb6a-8f04862dc585.jpg?1562910527"
        ruling("2004-10-04", "The number of cards in hand is checked both at the beginning of upkeep and when the triggered ability resolves.")
    }
}

/** "If that player has [count] or fewer cards in hand" — the player whose upkeep triggered. */
private fun upkeepPlayerHandAtMost(count: Int) = Compare(
    DynamicAmount.Count(Player.TriggeringPlayer, Zone.HAND),
    ComparisonOperator.LTE,
    DynamicAmount.Fixed(count)
)
