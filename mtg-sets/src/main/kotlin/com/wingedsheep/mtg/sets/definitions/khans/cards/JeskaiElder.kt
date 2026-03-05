package com.wingedsheep.mtg.sets.definitions.khans.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect

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
    typeLine = "Creature — Human Monk"
    power = 1
    toughness = 2
    oracleText = "Prowess (Whenever you cast a noncreature spell, this creature gets +1/+1 until end of turn.)\nWhenever Jeskai Elder deals combat damage to a player, you may draw a card. If you do, discard a card."

    prowess()

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = MayEffect(Effects.Loot())
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "Craig J Spearing"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bff9907c-4090-4dcc-aaf5-bc2a8dacce8b.jpg?1562792976"
    }
}
