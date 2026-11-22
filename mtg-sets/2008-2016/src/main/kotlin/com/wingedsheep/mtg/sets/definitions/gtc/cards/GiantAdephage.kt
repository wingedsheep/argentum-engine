package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Giant Adephage
 * {5}{G}{G}
 * Creature — Insect
 * 7/7
 * Trample
 * Whenever this creature deals combat damage to a player, create a token that's a copy of this creature.
 */
val GiantAdephage = card("Giant Adephage") {
    manaCost = "{5}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Insect"
    oracleText = "Trample\n" +
        "Whenever this creature deals combat damage to a player, create a token that's a copy of this creature."
    power = 7
    toughness = 7

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.CreateTokenCopyOfSelf()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "121"
        artist = "Christine Choi"
        flavorText = "\"To a creature like that, we must seem like, well, bugs.\"\n—Dars Gostok, Firefist captain"
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8bae725f-e582-4377-a855-51af035cdac3.jpg"
    }
}
