package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DiscardCardsEffect
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.Player

/**
 * Headhunter
 * {1}{B}
 * Creature — Human Cleric
 * 1/1
 * Whenever Headhunter deals combat damage to a player, that player discards a card.
 * Morph {B}
 */
val Headhunter = card("Headhunter") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "Whenever Headhunter deals combat damage to a player, that player discards a card.\nMorph {B}"

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = DiscardCardsEffect(1, EffectTarget.PlayerRef(Player.Opponent))
    }

    morph = "{B}"

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "156"
        artist = "Matt Cavotta"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/large/front/3/c/3cbd82d5-d64f-4833-b1a9-9652fcfa1578.jpg?1562909285"
    }
}
