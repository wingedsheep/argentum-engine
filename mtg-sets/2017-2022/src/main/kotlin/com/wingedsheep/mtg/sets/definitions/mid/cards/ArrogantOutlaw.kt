package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arrogant Outlaw
 * {2}{B}
 * Creature — Vampire Noble
 * 3/2
 * When this creature enters, if an opponent lost life this turn, each opponent loses 2 life and
 * you gain 2 life.
 *
 * Intervening-if ETB ([Conditions.OpponentLostLifeThisTurn], CR 603.4) draining each opponent.
 */
val ArrogantOutlaw = card("Arrogant Outlaw") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Noble"
    power = 3
    toughness = 2
    oracleText = "When this creature enters, if an opponent lost life this turn, each opponent loses 2 life " +
        "and you gain 2 life."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.OpponentLostLifeThisTurn
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "Aurore Folny"
        flavorText = "The debased remnants of the Falkenrath bloodline prowl the passes near their ruined castles."
        imageUri = "https://cards.scryfall.io/normal/front/4/9/496a9d45-6197-44b6-b0fc-17aeae281b2f.jpg"
    }
}
