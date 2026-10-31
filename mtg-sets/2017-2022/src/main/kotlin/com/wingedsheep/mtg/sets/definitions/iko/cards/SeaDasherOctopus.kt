package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Sea-Dasher Octopus
 * {1}{U}{U}
 * Creature — Octopus
 * 2/2
 * Mutate {1}{U}
 * Flash
 * Whenever this creature deals combat damage to a player, draw a card.
 */
val SeaDasherOctopus = card("Sea-Dasher Octopus") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Octopus"
    power = 2
    toughness = 2
    oracleText = "Mutate {1}{U} (If you cast this spell for its mutate cost, put it over or under target non-Human creature you own. They mutate into the creature on top plus all abilities from under it.)\n" +
        "Flash\n" +
        "Whenever this creature deals combat damage to a player, draw a card."

    keywordAbility(KeywordAbility.mutate("{1}{U}"))
    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "66"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a6f59d9e-6c1b-437a-934c-7c776c8dd1d5.jpg?1783931070"
    }
}
