package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fevered Visions
 * {1}{U}{R}
 * Enchantment
 * At the beginning of each player's end step, that player draws a card. If the player is your
 * opponent and has four or more cards in hand, this enchantment deals 2 damage to that player.
 *
 * "The player" is the one whose end step it is, so it is the active player while the trigger
 * resolves: "is your opponent" is [Conditions.IsOpponentsTurn] (read against this ability's
 * controller). Both halves of the condition are checked after the draw, as the ruling requires.
 */
val FeveredVisions = card("Fevered Visions") {
    manaCost = "{1}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each player's end step, that player draws a card. If the player is " +
        "your opponent and has four or more cards in hand, this enchantment deals 2 damage to that player."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        effect = Effects.DrawCards(1, EffectTarget.PlayerRef(Player.TriggeringPlayer)) then
            Effects.If(
                condition = Conditions.All(
                    Conditions.IsOpponentsTurn,
                    Conditions.CompareAmounts(
                        DynamicAmounts.count(Player.TriggeringPlayer, Zone.HAND),
                        ComparisonOperator.GTE,
                        4
                    )
                ),
                then = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "244"
        artist = "Steven Belledin"
        flavorText = "The mind can only expand so far before it flies apart."
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3ee3fc2b-c2e4-421d-ac1d-6a111bb44f44.jpg?1783937715"
        ruling(
            "2016-04-08",
            "No player may take any action in between the two steps of Fevered Visions's triggered ability, so " +
                "if your opponent has four or more cards in hand after drawing a card, Fevered Visions will deal " +
                "2 damage to that player."
        )
    }
}
