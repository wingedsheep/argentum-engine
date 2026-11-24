package com.wingedsheep.mtg.sets.definitions.rtr.cards

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
 * Shrieking Affliction
 * {B}
 * Enchantment
 *
 * At the beginning of each opponent's upkeep, if that player has one or fewer cards in hand,
 * they lose 3 life.
 *
 * Intervening-if: the hand size is checked when the trigger would fire and again on resolution.
 * Same shape as Hellfire Mongrel.
 */
val ShriekingAffliction = card("Shrieking Affliction") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "At the beginning of each opponent's upkeep, if that player has one or fewer cards in hand, they lose 3 life."

    triggeredAbility {
        trigger = Triggers.anOpponent.beginningOf(Step.UPKEEP)
        // "That player" is the player whose upkeep it is — bound by the step trigger.
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.count(Player.TriggeringPlayer, Zone.HAND),
            ComparisonOperator.LTE,
            1,
        )
        effect = Effects.LoseLife(3, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "76"
        artist = "Johann Bodin"
        flavorText = "A gargoyle's voice reaches pitches that will shatter glass and warp bone."
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfd08894-2534-4114-9365-40809ba95eb2.jpg?1783940360"
        ruling("2012-10-01", "Shrieking Affliction's ability will trigger only if an opponent begins their upkeep with one or fewer cards in hand. The ability will check the number of cards in that player's hand again when it tries to resolve. If that player has two or more cards in hand at that time, that player won't lose life.")
    }
}
