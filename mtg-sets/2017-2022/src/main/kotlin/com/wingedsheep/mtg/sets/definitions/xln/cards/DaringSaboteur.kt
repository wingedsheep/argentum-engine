package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Daring Saboteur
 * {1}{U}
 * Creature — Human Pirate
 * 2/1
 * {2}{U}: This creature can't be blocked this turn.
 * Whenever this creature deals combat damage to a player, you may draw a card. If you do, discard a card.
 */
val DaringSaboteur = card("Daring Saboteur") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Pirate"
    oracleText = "{2}{U}: This creature can't be blocked this turn.\n" +
        "Whenever this creature deals combat damage to a player, you may draw a card. If you do, discard a card."
    power = 2
    toughness = 1

    activatedAbility {
        cost = Costs.Mana("{2}{U}")
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.May(Patterns.Hand.loot())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "49"
        artist = "Victor Adame Minguez"
        flavorText = "\"They'll never see me coming.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57cb012c-947c-4281-8619-1384acf6df54.jpg?1783935785"
        ruling("2020-11-10", "Activating the first ability of Daring Saboteur after it has become blocked won't cause it to become unblocked.")
    }
}
