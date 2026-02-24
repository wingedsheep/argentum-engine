package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateGlobalTriggeredAbilityUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.dsl.Triggers

/**
 * False Cure
 * {B}{B}
 * Instant
 * Until end of turn, whenever a player gains life, that player loses 2 life
 * for each 1 life they gained.
 */
val FalseCure = card("False Cure") {
    manaCost = "{B}{B}"
    typeLine = "Instant"

    spell {
        effect = CreateGlobalTriggeredAbilityUntilEndOfTurnEffect(
            ability = TriggeredAbility.create(
                trigger = Triggers.AnyPlayerGainsLife.event,
                binding = Triggers.AnyPlayerGainsLife.binding,
                effect = LoseLifeEffect(
                    amount = DynamicAmount.Multiply(DynamicAmount.TriggerLifeGainAmount, 2),
                    target = EffectTarget.PlayerRef(Player.TriggeringPlayer)
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "146"
        artist = "Bradley Williams"
        flavorText = "\"The cure of a healer is like the blessing of a tyrant.\"\n—Phage the Untouchable"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef397db1-2d99-4cb0-a6e9-6f72d615ebad.jpg?1562951786"
    }
}
