package com.wingedsheep.mtg.sets.definitions.ddn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Jeskai Elder
 * {1}{U}
 * Creature — Human Monk
 * 1/2
 * Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)
 * Whenever Jeskai Elder deals combat damage to a player, you may draw a card. If you do, discard a card.
 */
val JeskaiElder = card("Jeskai Elder") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Monk"
    power = 1
    toughness = 2
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\nWhenever Jeskai Elder deals combat damage to a player, you may draw a card. If you do, discard a card."

    prowess()

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.May(Patterns.Hand.loot())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "Craig J Spearing"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea4ff6f3-cc53-4f4a-b884-2e751732f9c8.jpg?1783939113"
    }
}
