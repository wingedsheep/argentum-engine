package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreateGlobalTriggeredAbilityUntilEndOfTurnEffect
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.LoseLifeEffect
import com.wingedsheep.sdk.scripting.OnLifeGain
import com.wingedsheep.sdk.scripting.Player
import com.wingedsheep.sdk.scripting.TriggeredAbility

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
                trigger = OnLifeGain(controllerOnly = false),
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
        imageUri = "https://cards.scryfall.io/large/front/e/f/ef397db1-2d99-4cb0-a6e9-6f72d615ebad.jpg?1562951786"
    }
}
