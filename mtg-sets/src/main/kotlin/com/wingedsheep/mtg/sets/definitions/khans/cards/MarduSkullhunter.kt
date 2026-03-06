package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.conditions.YouAttackedThisTurn
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Mardu Skullhunter
 * {1}{B}
 * Creature — Human Warrior
 * 2/1
 * Mardu Skullhunter enters the battlefield tapped.
 * Raid — When Mardu Skullhunter enters the battlefield, if you attacked this turn,
 * target opponent discards a card.
 */
val MarduSkullhunter = card("Mardu Skullhunter") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Human Warrior"
    power = 2
    toughness = 1
    oracleText = "This creature enters tapped.\nRaid — When this creature enters, if you attacked this turn, target opponent discards a card."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = YouAttackedThisTurn
        val t = target("target opponent", TargetOpponent())
        effect = EffectPatterns.discardCards(1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd3ca5e7-96f3-4326-9315-34bb396a054c.jpg?1562794625"
    }
}
