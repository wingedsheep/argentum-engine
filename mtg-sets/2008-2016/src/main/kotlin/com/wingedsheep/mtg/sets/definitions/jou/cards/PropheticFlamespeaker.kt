package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Prophetic Flamespeaker
 * {1}{R}{R}
 * Creature — Human Shaman
 * 1/3
 *
 * Double strike, trample
 * Whenever this creature deals combat damage to a player, exile the top card of your library.
 * You may play it this turn.
 *
 * The payoff is plain impulse draw ([Patterns.Exile.impulse]). With double strike it triggers once
 * per combat damage step that damages a player, so an unblocked (or trampling-over) Flamespeaker
 * exiles two cards in one combat.
 */
val PropheticFlamespeaker = card("Prophetic Flamespeaker") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    power = 1
    toughness = 3
    oracleText = "Double strike, trample\n" +
        "Whenever this creature deals combat damage to a player, exile the top card of your library. " +
        "You may play it this turn."

    keywords(Keyword.DOUBLE_STRIKE, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Patterns.Exile.impulse(count = 1)
        description = "Whenever this creature deals combat damage to a player, exile the top card of " +
            "your library. You may play it this turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "106"
        artist = "Cynthia Sheppard"
        flavorText = "Fire to destroy. Fire to create."
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73326c79-f884-42d5-8546-3db46ce4fe52.jpg?1783939422"
    }
}
