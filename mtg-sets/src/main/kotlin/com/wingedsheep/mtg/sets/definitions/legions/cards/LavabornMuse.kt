package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageToPlayersEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

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
    typeLine = "Creature — Spirit"
    oracleText = "At the beginning of each opponent's upkeep, if that player has two or fewer cards in hand, Lavaborn Muse deals 3 damage to that player."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.EachOpponentUpkeep
        triggerCondition = Conditions.OpponentCardsInHandAtMost(2)
        effect = ConditionalEffect(
            condition = Conditions.OpponentCardsInHandAtMost(2),
            effect = DealDamageToPlayersEffect(3, EffectTarget.PlayerRef(Player.Opponent))
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
