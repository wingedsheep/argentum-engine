package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Mindscour Dragon — Fate Reforged #41
 * {4}{U}{U} · Creature — Dragon · 4/4
 *
 * Flying
 * Whenever this creature deals combat damage to an opponent, target player mills four cards.
 *
 * "To an opponent" is [Recipient.Opponent]: combat damage to a planeswalker or battle doesn't
 * trigger it. The milled player is any target player, not necessarily the one damaged.
 */
val MindscourDragon = card("Mindscour Dragon") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\nWhenever this creature deals combat damage to an opponent, target player " +
        "mills four cards."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.Opponent)
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(4, player)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "41"
        artist = "Wesley Burt"
        flavorText = "\"The bells may one day ring as an anthem of our own end.\"\n—Kyu, Sage-Eye mystic"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5708290-60ae-400c-b149-2a63153c2890.jpg?1783938705"
    }
}
