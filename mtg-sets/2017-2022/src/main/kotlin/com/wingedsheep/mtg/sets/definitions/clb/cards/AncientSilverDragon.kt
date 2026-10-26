package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Ancient Silver Dragon
 * {6}{U}{U}
 * Creature — Elder Dragon
 * 8/8
 * Flying
 * Whenever this creature deals combat damage to a player, roll a d20. Draw cards equal to the
 * result. You have no maximum hand size for the rest of the game.
 *
 * The roll's result is captured with `rollDiceResult` and feeds a dynamic draw; the hand-size
 * clause is the player-scoped, permanent `RemoveMaximumHandSize` (it outlives the Dragon).
 */
val AncientSilverDragon = card("Ancient Silver Dragon") {
    manaCost = "{6}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elder Dragon"
    power = 8
    toughness = 8
    oracleText = "Flying\n" +
        "Whenever this creature deals combat damage to a player, roll a d20. Draw cards equal to the " +
        "result. You have no maximum hand size for the rest of the game."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.Pipeline {
            val result = rollDiceResult(sides = 20)
            run(Effects.DrawCards(result.amount))
            run(Effects.RemoveMaximumHandSize())
        }
        description = "Whenever this creature deals combat damage to a player, roll a d20. Draw cards " +
            "equal to the result. You have no maximum hand size for the rest of the game."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "56"
        artist = "Raoul Vitale"
        imageUri = "https://cards.scryfall.io/normal/front/c/e/ced18935-4ce9-466e-a88d-2b14ff8f989f.jpg?1783922795"
    }
}
