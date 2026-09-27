package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Seizan, Perverter of Truth
 * {3}{B}{B}
 * Legendary Creature — Demon Spirit
 * 6/5
 * At the beginning of each player's upkeep, that player loses 2 life and draws two cards.
 *
 * "That player" is the player whose upkeep it is, bound by the each-player step trigger as
 * [Player.TriggeringPlayer] — Seizan's controller included.
 */
val SeizanPerverterOfTruth = card("Seizan, Perverter of Truth") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Demon Spirit"
    oracleText = "At the beginning of each player's upkeep, that player loses 2 life and draws two cards."
    power = 6
    toughness = 5

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.TriggeringPlayer)) then
            Effects.DrawCards(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "143"
        artist = "Kev Walker"
        flavorText = "\"If you would taste the wisdom of the oni, be prepared to salt it with your blood.\"\n—Kiku, Night's Flower"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1e61750-f7d7-4e6d-9d68-e1e357868a8d.jpg?1783944308"
    }
}
